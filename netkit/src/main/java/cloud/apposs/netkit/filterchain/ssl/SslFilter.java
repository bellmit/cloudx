package cloud.apposs.netkit.filterchain.ssl;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * HTTP SSL加密、解密通讯过滤器
 */
public class SslFilter extends IoFilterAdaptor {
    public static final String PROTOCOL_SSL = "TLS";
    /**
     * 握手超时时间
     */
    private static final int HANDSHAKE_TIMEOUT = 2000;

    public static final String FILTER_CONTEXT = "SslFilterContext";

    private SSLContext context;

    private boolean useClientMode = true;

    private int bufferSize = 2 * 1024;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * 是否在建立连接后读取远程服务数据，
     * http协议在建立之后是马上发送数据需要设置为false，
     * mail协议在建立之后要读取远程服务需要设置为true
     */
    private boolean ignoreWelcome;

    public SslFilter(boolean useClientMode, boolean ignoreWelcome) throws Exception {
        this(SSLContext.getInstance(PROTOCOL_SSL), useClientMode, ignoreWelcome);
    }

    public SslFilter(String sslKeystore, String sslPassword,
                     boolean useClientMode, boolean ignoreWelcome) throws Exception {
        this(initSSLContext(sslKeystore, sslPassword), useClientMode, ignoreWelcome);
    }

    public SslFilter(SSLContext context, boolean useClientMode, boolean ignoreWelcome) throws Exception {
        this.context = context;
        if (useClientMode) {
            this.context.init(null, X509_MANAGERS, null);
        }
        this.useClientMode = useClientMode;
        this.ignoreWelcome = ignoreWelcome;
    }

    @Override
    public void channelConnect(NextFilter nextFilter, IoProcessor processor) throws Exception {
        Context context = getContext(processor);
        IoBuffer buf = context.onConnect(processor);
        // 握手的过程中产生了数据粘包，
        // 如果业务方需要在建立连接后获取信息即ignoreWelcome=false则改成channelRead传递实现数据读逻辑
        if (buf != null && !ignoreWelcome) {
            nextFilter.channelRead(processor, buf);
        } else {
            nextFilter.channelConnect(processor);
        }
    }

    @Override
    public void channelAccept(NextFilter nextFilter, IoProcessor processor,
                              EventChannel channel) throws Exception {
        Context context = getContext(processor);
        context.onAccept(processor);
        nextFilter.channelAccept(processor, channel);
    }

    @Override
    public void channelRead(NextFilter nextFilter, IoProcessor processor,
                            Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }

        final Context context = getContext(processor);
        final IoBuffer in = (IoBuffer) message;
        final IoBuffer buffer = context.onRead(in);
        nextFilter.channelRead(processor, buffer);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoProcessor processor,
                            IoBuffer buffer) throws Exception {
        final Context context = getContext(processor);
        final IoBuffer out = context.onWrite(buffer);
        nextFilter.filterWrite(processor, out);
    }

    @Override
    public void channelSend(NextFilter nextFilter, IoProcessor processor,
                            WriteRequest writeRequest) throws Exception {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        // 数据发送完毕，重置资源
        if (context != null) {
            context.reset();
        }
        nextFilter.channelSend(processor, writeRequest);
    }

    @Override
    public void channelClose(NextFilter nextFilter, IoProcessor processor) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        // 数据发送完毕，释放资源
        if (context != null) {
            context.release();
        }
        nextFilter.channelClose(processor);
    }

    private Context getContext(IoProcessor processor) throws SSLException {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);

        if (context == null) {
            context = new Context(this.context, useClientMode);
            processor.setAttribute(FILTER_CONTEXT, context);
        }

        return context;
    }

    public class Context {
        private SSLEngine sslEngine;
        private HandshakeStatus handshakeStatus;
        private boolean handshakeDone = false;

        private ByteBuffer netIn;
        private ByteBuffer appIn;
        private ByteBuffer netOut;
        private ByteBuffer emptyBuffer = ByteBuffer.allocate(0);

        /**
         * 数据接收/发送时的解密/加密数据
         */
        private IoBuffer onReadBuf;
        private IoBuffer onSendBuf;

        public Context(SSLContext context, boolean useClientMode) throws SSLException {
            SSLEngine sslEngine = context.createSSLEngine();
            if (useClientMode) {
                sslEngine.setUseClientMode(true);
            } else {
                sslEngine.setUseClientMode(false);
                sslEngine.setNeedClientAuth(false);
            }
            sslEngine.beginHandshake();
            this.sslEngine = sslEngine;
            this.handshakeStatus = sslEngine.getHandshakeStatus();
        }

        public IoBuffer onAccept(final IoProcessor processor) throws IOException {
            final EventChannel channel = processor.getChannel();
            return doHandshakeLoop(channel, processor.getFlow());
        }

        public IoBuffer onConnect(final IoProcessor processor) throws IOException {
            final EventChannel channel = processor.getChannel();
            // 客户端模式是先发送握手数据
            doHandshake(channel);
            return doHandshakeLoop(channel, processor.getFlow());
        }

        private IoBuffer doHandshakeLoop(final EventChannel channel, long flow) throws IOException {
            Selector recvSelector = null;
            SelectionKey key = null;

            try {
                long beg = System.currentTimeMillis();
                while (!handshakeDone) {
                    long end = System.currentTimeMillis();
                    if ((end - beg) > HANDSHAKE_TIMEOUT) {
                        throw new IOException("handshake timeout;flow=" + flow);
                    }

                    ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                    long len = channel.recv(buf);
                    if (len < 0) {
                        throw new IOException("channel read error");
                    }
                    if (len == 0) {
                        // 如果接收数据为0注册读事件到selector让内核通知是否有数据可读，避免cpu空转
                        if (recvSelector == null) {
                            recvSelector = Selector.open();
                            key = channel.register(recvSelector, SelectionKey.OP_READ);
                        }
                        recvSelector.select(HANDSHAKE_TIMEOUT);
                        continue;
                    }

                    buf.flip();
                    if (netIn == null) {
                        netIn = ByteBuffer.allocate(buf.remaining());
                    }
                    // 如果buf容量不够，需要进行扩容再存储网络数据
                    if (netIn.remaining() < buf.remaining()) {
                        ByteBuffer newBuf = ByteBuffer.allocate(netIn.limit() + buf.remaining());
                        netIn.flip();
                        newBuf.put(netIn);
                        netIn = newBuf;
                    }
                    netIn.put(buf);
                    doHandshake(channel);
                }

                // 握手操作中因为网络问题可能远端服务会连同发送数据也发送过来产生数据粘包
                appIn.flip();
                if (appIn.hasRemaining()) {
                    IoBuffer appInBuf = new ByteBuf(appIn.remaining(), true);
                    appInBuf.put(appIn);
                    appIn = null;
                    return appInBuf;
                }
            } finally {
                if (key != null) {
                    key.cancel();
                    key = null;
                }
                if (recvSelector != null) {
                    recvSelector.selectNow();
                    recvSelector.close();
                    recvSelector = null;
                }
            }

            return null;
        }

        public IoBuffer onRead(final IoBuffer in) throws IOException {
            // 对数据进行解密
            SSLSession sslSession = sslEngine.getSession();
            int appBufferMax = sslSession.getApplicationBufferSize();
            if (onReadBuf == null) {
                onReadBuf = IoAllocator.allocate(appBufferMax);
            }

            if (appIn == null) {
                appIn = ByteBuffer.allocate(appBufferMax);
            }
            SSLEngineResult engineResult = null;
            HandshakeStatus handshakeStatus = null;
            SSLEngineResult.Status status = null;
            ByteBuffer allBuf = null;
            if (netIn != null) {
                allBuf = ByteBuffer.allocateDirect((int) (in.readableBytes() + netIn.remaining()));
                allBuf.put(netIn);
                ByteBuffer inBuf = in.buffer();
                inBuf.flip();
                allBuf.put(inBuf);
                netIn = null;
            } else {
                allBuf = ByteBuffer.allocateDirect((int) in.readableBytes());
                ByteBuffer inBuf = in.buffer();
                inBuf.flip();
                allBuf.put(inBuf);
            }
            allBuf.flip();

            do {
                engineResult = sslEngine.unwrap(allBuf, appIn);
                status = engineResult.getStatus();
                handshakeStatus = engineResult.getHandshakeStatus();
                if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    ByteBuffer b = ByteBuffer.allocate(sslSession.getApplicationBufferSize() + appIn.position());
                    appIn.flip();
                    b.put(appIn);
                    appIn = b;
                    continue;
                }
            } while (
                    (
                            (status == SSLEngineResult.Status.OK)
                                    ||
                                    (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    )
                            &&
                            (
                                    (handshakeStatus == HandshakeStatus.NOT_HANDSHAKING)
                                            ||
                                            (handshakeStatus == HandshakeStatus.NEED_UNWRAP)
                            )
                    );

            appIn.flip();
            onReadBuf.put(appIn);
            appIn = null;
            netIn = null;
            // 此时接收的数据还有残余数据未完全解包，需要存储下来等待下次数据接收时和接收的数据一起再进行解包
            if (allBuf.remaining() > 0) {
                netIn = ByteBuffer.allocate(allBuf.remaining());
                netIn.put(allBuf);
                netIn.flip();
            }

            return onReadBuf;
        }

        public IoBuffer onWrite(final IoBuffer out) throws IOException {
            // 对要数据的数据先进行SSL加密再发送
            if (onSendBuf == null) {
                onSendBuf = IoAllocator.allocate(bufferSize);
            }
            while (out.hasReadableBytes()) {
                SSLSession sslSession = sslEngine.getSession();
                int netBufferMax = sslSession.getPacketBufferSize();
                ByteBuffer netOutBuf = ByteBuffer.allocate(netBufferMax);
                int outBufLen = (int) (out.readableBytes() < bufferSize ? out.readableBytes() : bufferSize);
                ByteBuffer tmpBuf = out.buffer(out.readIdx(), outBufLen);
                tmpBuf.flip();
                SSLEngineResult engineResult = sslEngine.wrap(tmpBuf, netOutBuf);
                if (engineResult.getStatus() == SSLEngineResult.Status.CLOSED) {
                    throw new IOException("channel ssl closed error;");
                }
                out.readIdx(out.readIdx() + tmpBuf.position());
                netOutBuf.flip();
                onSendBuf.put(netOutBuf);
            }
            return onSendBuf;
        }

        public void reset() {
            if (onReadBuf != null) {
                onReadBuf.reset();
            }
            if (onSendBuf != null) {
                // 数据发送内存不缓存了，
                // 因为上层应用如EventLoop可能会直接引用该变量并且在发送完数据之后直接释放内存
                onSendBuf.free();
                onSendBuf = null;
            }
        }

        public void release() {
            if (onReadBuf != null) {
                onReadBuf.free();
                onReadBuf = null;
            }
            if (onSendBuf != null) {
                onSendBuf.free();
                onSendBuf = null;
            }
        }

        private int doHandshake(final EventChannel channel) throws IOException {
            int sendLen = 0;
            for (; ; ) {
                switch (handshakeStatus) {
                    case FINISHED:
                        handshakeDone = true;
                        return sendLen;
                    case NEED_TASK:
                        handshakeStatus = doTasks();
                        break;
                    case NEED_UNWRAP:
                        SSLEngineResult.Status status = unwrapHandshake();
                        if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW
                                && handshakeStatus != HandshakeStatus.FINISHED || sslEngine.isInboundDone()) {
                            return sendLen;
                        }
                        break;
                    case NEED_WRAP:
                        if (netOut != null && netOut.hasRemaining()) {
                            return sendLen;
                        }

                        SSLSession session = sslEngine.getSession();
                        SSLEngineResult result;
                        netOut = ByteBuffer.allocateDirect(session.getPacketBufferSize());
                        for (; ; ) {
                            result = sslEngine.wrap(emptyBuffer, netOut);
                            if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                                ByteBuffer newBuf = ByteBuffer.allocate(netOut.capacity() << 1);
                                netOut.rewind();
                                newBuf.put(netOut);
                                netOut = newBuf;
                            } else {
                                break;
                            }
                        }

                        netOut.flip();
                        handshakeStatus = result.getHandshakeStatus();
                        sendLen += channel.send(netOut);
                        break;
                    default:
                        throw new IllegalStateException("invalid handshaking state" + handshakeStatus);
                }
            }
        }

        private HandshakeStatus doTasks() {
            Runnable runnable;
            while ((runnable = sslEngine.getDelegatedTask()) != null) {
                runnable.run();
            }
            return sslEngine.getHandshakeStatus();
        }

        private SSLEngineResult.Status unwrapHandshake() throws SSLException {
            if (netIn != null) {
                netIn.flip();
            }
            if (netIn == null || !netIn.hasRemaining()) {
                return SSLEngineResult.Status.BUFFER_UNDERFLOW;
            }

            SSLEngineResult res = unwrap();
            handshakeStatus = res.getHandshakeStatus();

            if (handshakeStatus == HandshakeStatus.FINISHED && res.getStatus() == SSLEngineResult.Status.OK && netIn.hasRemaining()) {
                res = unwrap();
                if (netIn.hasRemaining()) {
                    netIn.compact();
                } else {
                    netIn = null;
                }
            } else {
                if (netIn.hasRemaining()) {
                    netIn.compact();
                } else {
                    netIn = null;
                }
            }

            return res.getStatus();
        }

        private SSLEngineResult unwrap() throws SSLException {
            if (appIn == null) {
                appIn = ByteBuffer.allocate(netIn.remaining());
            }
            SSLEngineResult res;
            SSLEngineResult.Status status = null;
            HandshakeStatus handshakeStatus = null;

            do {
                res = sslEngine.unwrap(netIn, appIn);
                status = res.getStatus();
                handshakeStatus = res.getHandshakeStatus();
                if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    ByteBuffer newBuf = ByteBuffer.allocate(appIn.capacity() << 1);
                    appIn.rewind();
                    newBuf.put(appIn);
                    appIn = newBuf;
                    continue;
                }
            } while (
                    (
                            (status == SSLEngineResult.Status.OK)
                                    ||
                                    (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    )
                            &&
                            (
                                    (handshakeStatus == HandshakeStatus.NOT_HANDSHAKING)
                                            ||
                                            (handshakeStatus == HandshakeStatus.NEED_UNWRAP)
                            )
                    );
            return res;
        }
    }

    public static SSLContext initSSLContext(String sslKeystore, String sslPassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        char[] passphrase = sslPassword.toCharArray();
        ks.load(new FileInputStream(sslKeystore), passphrase);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        SSLContext sslContext = SSLContext.getInstance(PROTOCOL_SSL);
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }

    public static final X509TrustManager X509 = new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] x509Certificates,
                                       String s) throws CertificateException {
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates,
                                       String s) throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    public static final TrustManager[] X509_MANAGERS = new TrustManager[]{X509};
}
