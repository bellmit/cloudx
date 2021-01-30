package cloud.apposs.netkit.filterchain.websocket;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpParseException;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.LineBuilder;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;

/**
 * websocket协议解析，参考：
 * <pre>
 *     https://www.cnblogs.com/skyay/p/9382037.html
 *     https://segmentfault.com/a/1190000013298527
 * </pre>
 */
public class WebSocketFilter extends IoFilterAdaptor {
    public static final String FILTER_NAME = "WebSocketFilter";
    public static final String FILTER_CONTEXT = "WebSocketFilterContext";
    public static final String FILTER_SESSION_CONTEXT = "WebSocketFilterSessionContext";

    private final String charset;

    /**
     * 是否将HTTP请求的HEADER KEY自动转换成小写，便于提升性能
     */
    private final boolean lowerHeaderKey;

    public WebSocketFilter(String charset, boolean lowerHeaderKey) {
        super(FILTER_NAME);
        this.charset = charset;
        this.lowerHeaderKey = lowerHeaderKey;
    }

    @Override
    public void channelRead(NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }

        IoBuffer buffer = (IoBuffer) message;
        Context context = getContext(processor);
        // WS协议一开始是要先握手通讯
        if (context.isHandshakeComplete()) {
            // 已经握手成功则需要进行内容数据解码
            if (context.payload(buffer)) {
                // 特殊的数据帧需要做特殊处理，例如CLOSE/PING/PONG类型的数据帧
                if (!doHandleSpecialFrame(processor, context.getFrame())) {
                    // 只有数据解码完毕才交给下一个节点处理
                    nextFilter.channelRead(processor, context.getFrame());
                }
            }
        } else {
            // 没握手则先进行握手
            if (context.handshake(buffer)) {
                // 发送服务端响应数据给客户端建立WS连接
                processor.setAttribute(FILTER_SESSION_CONTEXT, context.getSession());
                nextFilter.channelRead(processor, true);
            }
        }
    }

    @Override
    public void channelClose(NextFilter nextFilter, IoProcessor processor) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        if (context != null) {
            context.release();
        }
        nextFilter.channelClose(processor);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoProcessor processor, Throwable cause) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        if (context != null) {
            context.release();
        }
        nextFilter.exceptionCaught(processor, cause);
    }

    private Context getContext(IoProcessor processor) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);

        if (context == null) {
            SocketAddress remoteAddr = processor.getChannel().getRemoteSocketAddress();
            context = new Context(processor, remoteAddr, charset, lowerHeaderKey);
            processor.setAttribute(FILTER_CONTEXT, context);
        }

        return context;
    }

    /**
     * 一个请求就是一个上下文实例
     */
    public static final class Context {
        /** 解析请求行的各种状态 */
        /**
         * 初始状态，解析WS握手协议
         */
        public static final int HANDSHAKE_STATUS_ROTOCOL = 0;
        /**
         * 解析WS握手请求头
         */
        public static final int HANDSHAKE_STATUS_HEADER = 1;
        /**
         * WS握手结束
         */
        public static final int HANDSHAKE_STATUS_FINISH = 2;
        /**
         * 包体解析开始阶段
         */
        public static final int PAYLOAD_STATUS_START = 3;
        public static final int PAYLOAD_STATUS_LEN = 4;
        public static final int PAYLOAD_STATUS_EXTLEN = 5;
        public static final int PAYLOAD_STATUS_MASKING_KEY = 6;
        public static final int PAYLOAD_STATUS_BODY = 7;

        /** 协议体解析 */
        /** 1000 0000 */
        public static final byte MASK = 0x1;
        public static final byte HAS_EXTEND_DATA = 126;
        public static final byte HAS_EXTEND_DATA_CONTINUE = 127;
        /** 0111 1111 */
        public static final byte PAYLOADLEN = 0x7F;

        public static final int DEFAULT_LINE_LENGTH = 128;

        /**
         * 是否将HTTP请求的HEADER KEY自动转换成小写，便于提升性能
         */
        private boolean lowerHeaderKey = false;

        private String charset;

        /**
         * 当前解析状态
         */
        private int status = HANDSHAKE_STATUS_ROTOCOL;

        private final WebSocketSession session;

        private WebSocketFrame frame;

        /**
         * 解析出来的当前行
         */
        private final LineBuilder currentLine;
        /**
         * 协议数据解析的中间状态
         */
        boolean isMask;
        int payloadLength;
        int currentPayloadLenIndex = 0;
        byte[] payloadFrame;
        int currentMaskingKeyIndex = 0;
        byte[] maskingKey;
        int currentWriteBufferIndex = 0;

        public Context(IoProcessor processor, SocketAddress remoteAddr, String charset, boolean lowerHeaderKey) {
            this.currentLine = new LineBuilder(DEFAULT_LINE_LENGTH, charset);
            this.lowerHeaderKey = lowerHeaderKey;
            this.charset = charset;
            this.session = new WebSocketSession(processor, remoteAddr);
            this.session.setCharset(charset);
        }

        public WebSocketSession getSession() {
            return session;
        }

        public WebSocketFrame getFrame() {
            return frame;
        }

        /**
         * 判断是否已经握手结束，进入数据体解包阶段
         */
        public boolean isHandshakeComplete() {
            return status >= HANDSHAKE_STATUS_FINISH;
        }

        /**
         * 解析请求HEADER行，WS协议握手阶段值如下
         * <pre>
         *     GET / HTTP/1.1
         *     Host: 172.17.1.206:1984
         *     Connection: Upgrade
         *     Pragma: no-cache
         *     Cache-Control: no-cache
         *     Upgrade: websocket
         *     Origin: ws://172.17.1.206:1984
         *     Sec-WebSocket-Version: 13
         *     User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36
         *     Accept-Encoding: gzip, deflate
         * </pre>
         */
        public boolean handshake(IoBuffer buffer) throws Exception {
            String line = null;
            switch (status) {
                case HANDSHAKE_STATUS_ROTOCOL:
                    // 解析请求协议，即：GET / HTTP/1.1
                    line = doParseLine(buffer);
                    if (line == null) {
                        // 没解析到完整行，退出等待下次数据请求过来
                        return false;
                    }

                    // 获取解析协议
                    String[] protocols = line.split("\\s+");
                    if (protocols.length < 3) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400);
                    }
                    doInitRequestProtocol(session, protocols);
                    status = HANDSHAKE_STATUS_HEADER;
                case HANDSHAKE_STATUS_HEADER:
                    // 解析请求头，即：Connection: Upgrade
                    do {
                        line = doParseLine(buffer);
                        if (line == null) {
                            // 没解析到完整行，退出等待下次数据请求过来
                            return false;
                        }
                        String[] arrs = line.split(":");
                        if (arrs.length >= 2) {
                            String headerKey = arrs[0].trim();
                            if (lowerHeaderKey) {
                                headerKey = headerKey.toLowerCase();
                            }
                            session.putHeader(headerKey, StrUtil.joinArrayString(arrs, ":", 1));
                        }
                    } while (line.length() > 0);
                    // 已经解析到CRLF最后的空行，握手数据已经解析完毕，
                    // 服务端在客户端发送握手数据后也要响应结果给客户端，才进入主体数据传输阶段
                    status = HANDSHAKE_STATUS_FINISH;
            }
            return true;
        }

        /**
         * 数据协议交互，协议包定义如下：
         * <pre>
         *      0                   1                   2                   3
         *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         *     +-+-+-+-+-------+-+-------------+-------------------------------+
         *     |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
         *     |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
         *     |N|V|V|V|       |S|             |   (if payload len==126/127)   |
         *     | |1|2|3|       |K|             |                               |
         *     +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
         *     |    Extended payload length continued, if payload len == 127   |
         *     + - - - - - - - - - - - - - - - +-------------------------------+
         *     |                               | Masking-key, if MASK set to 1 |
         *     +-------------------------------+-------------------------------+
         *     |    Masking-key (continued)    |          Payload Data         |
         *     +-------------------------------- - - - - - - - - - - - - - - - +
         *     :                     Payload Data continued ...                :
         *     + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
         *     |                     Payload Data continued ...                |
         *     +---------------------------------------------------------------+
         * </pre>
         */
        public boolean payload(IoBuffer buffer) throws Exception {
            if (status <= HANDSHAKE_STATUS_FINISH) {
                status = PAYLOAD_STATUS_START;
            }
            for (; ; ) {
                if (!buffer.hasReadableBytes()) {
                    // 已经没有可读取的数据，退出等待接收下次网络数据
                    return false;
                }
                switch (status) {
                    case PAYLOAD_STATUS_START:
                        // 第一个字节，判断是否有后续数据包，以及对应的OPCODE类型
                        byte value = buffer.get();
                        boolean isFinalFragment = (value >> 7) > 0;
                        int frameOpcode = value & 0x0F;
                        frame = new WebSocketFrame(isFinalFragment, frameOpcode, charset);
                        status = PAYLOAD_STATUS_LEN;
                        continue;
                    case PAYLOAD_STATUS_LEN:
                        // 第二个字节，第一位存放掩码（1:有掩码；0:无掩码），剩下的后面7位用来描述消息长度
                        // 由于7位最多只能描述127所以这个值会代表三种情况
                        // 第一种是消息内容少于126存储消息长度
                        // 第二种是消息长度大于等于126且少于UINT16的情况此值为126
                        // 第三种是消息长度大于UINT16的情况下此值为127
                        value = buffer.get();
                        payloadLength = (value & PAYLOADLEN);
                        isMask = (value >> 7 & MASK) > 0;
                        if (isMask) {
                            maskingKey = new byte[4];
                        }
                        if (payloadLength < HAS_EXTEND_DATA) {
                            status = isMask ? PAYLOAD_STATUS_MASKING_KEY : PAYLOAD_STATUS_BODY;
                        } else if (payloadLength == HAS_EXTEND_DATA) {
                            // 如果消息长度少于UINT16的情况此值为126，需要读取后面2个字节以获取payload长度
                            status = PAYLOAD_STATUS_EXTLEN;
                            payloadFrame = new byte[2];
                        } else if (payloadLength == HAS_EXTEND_DATA_CONTINUE) {
                            // 如果消息长度大于UINT16的情况下此值为127，需要读取后面4个字节以获取payload长度
                            status = PAYLOAD_STATUS_EXTLEN;
                            payloadFrame = new byte[4];
                        }
                        continue;
                    case PAYLOAD_STATUS_EXTLEN:
                        if (payloadLength == HAS_EXTEND_DATA) {
                            if (doParsePayloadLen(buffer, 2)) {
                                status = isMask ? PAYLOAD_STATUS_MASKING_KEY : PAYLOAD_STATUS_BODY;
                            }
                        } else if (payloadLength == HAS_EXTEND_DATA_CONTINUE) {
                            if (doParsePayloadLen(buffer, 4)) {
                                status = isMask ? PAYLOAD_STATUS_MASKING_KEY : PAYLOAD_STATUS_BODY;
                            }
                        }
                        continue;
                    case PAYLOAD_STATUS_MASKING_KEY:
                        // 解析Masking-Key
                        if (doParseMaskingKey(buffer)) {
                            // 荷载数据为0，可能数据帧是关闭类型
                            if (payloadLength <= 0) {
                                return true;
                            }
                            status = PAYLOAD_STATUS_BODY;
                        }
                        continue;
                    case PAYLOAD_STATUS_BODY:
                        // 解析荷载数据，初次获取数据体时先分配好内存数据
                        if (currentWriteBufferIndex <= 0) {
                            frame.allocate(payloadLength);
                        }
                        int total = payloadLength - currentWriteBufferIndex;
                        byte[] readBuffer;
                        if (buffer.readableBytes() < total) {
                            total = (int) buffer.readableBytes();
                        }
                        readBuffer = new byte[total];
                        buffer.get(readBuffer);
                        if (isMask) {
                            for (int i = 0; i < readBuffer.length; i++) {
                                // 数据进行异或运算
                                readBuffer[i] = (byte) (readBuffer[i] ^ maskingKey[i % 4]);
                            }
                        }
                        frame.write(readBuffer, currentWriteBufferIndex, total);
                        currentWriteBufferIndex += total;
                        if (currentWriteBufferIndex >= payloadLength) {
                            // 重置状态，等待接收下一个完整数据帧
                            isMask = false;
                            payloadLength = 0;
                            currentPayloadLenIndex = 0;
                            payloadFrame = null;
                            currentMaskingKeyIndex = 0;
                            maskingKey = null;
                            currentWriteBufferIndex = 0;
                            status = PAYLOAD_STATUS_START;
                            return true;
                        }
                        continue;
                }
            }
        }

        /**
         * 会话关闭，释放资源，包括数据清空和临时文件删除等
         */
        public void release() {
            if (session != null) {
                session.release();
            }
        }

        /**
         * 解析请求行，成功解析到一行返回true，否则返回false
         */
        private String doParseLine(IoBuffer buffer) throws Exception {
            long index = buffer.readIdx();
            long total = buffer.writeIdx();

            for (; index < total; index++) {
                byte letter = buffer.get();
                if (letter == HttpConstants.CR) {
                    continue;
                }
                if (letter == HttpConstants.LF) {
                    String line = currentLine.toString();
                    currentLine.setLength(0);
                    return line;
                }
                if (currentLine.length() > HttpConstants.MAX_HEADER_LINE) {
                    throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Max Header Line");
                }
                currentLine.append(letter);
            }

            return null;
        }

        /**
         * 解析请求协议，即：GET / HTTP/1.1
         */
        private void doInitRequestProtocol(WebSocketSession session, String[] protocols) {
            String method = protocols[0];
            String uri = protocols[1];
            String protocol = protocols[2];
            String path = uri;
            int pathEndPos = uri.indexOf('?');
            if (pathEndPos > 0) {
                path = uri.substring(0, pathEndPos);
                Map<String, String> parameters = session.getParameters();
                String[] arguments = uri.substring(path.length() + 1).split("&");
                for (int i = 0; i < arguments.length; i++) {
                    String argument = arguments[i];
                    String[] variables = argument.split("=");
                    if (variables.length == 2) {
                        parameters.put(variables[0].trim(), variables[1].trim());
                    }
                }
            }
            session.setMethod(method);
            session.setProtocol(protocol);
            session.setRequestUri(path);
        }

        /**
         * 解析数据帧payload长度，为了应付数据断包问题
         */
        private boolean doParsePayloadLen(IoBuffer buffer, int needLen) throws IOException {
            if (currentPayloadLenIndex > 0) {
                int total = buffer.readableBytes() > needLen - currentPayloadLenIndex ?
                        needLen - currentPayloadLenIndex : (int) buffer.readableBytes();
                buffer.get(payloadFrame,0, total);
                currentPayloadLenIndex += total;
            } else {
                if (buffer.readableBytes() >= needLen) {
                    buffer.get(payloadFrame);
                    currentPayloadLenIndex += needLen;
                } else {
                    int total = (int) buffer.readableBytes();
                    buffer.get(payloadFrame,0, total);
                    currentPayloadLenIndex += total;
                }
            }
            if (currentPayloadLenIndex < needLen - 1) {
                return false;
            }
            int shift = 0;
            int length = 0;
            for (int i = payloadFrame.length - 1; i >= 0; i--) {
                length = length + ((payloadFrame[i] & 0xFF) << shift);
                shift += 8;
            }
            payloadLength = length;
            return true;
        }

        /**
         * 解析数据帧Masking-Key，为了应付数据断包问题
         */
        private boolean doParseMaskingKey(IoBuffer buffer) throws IOException {
            if (currentMaskingKeyIndex > 0) {
                int total = buffer.readableBytes() > 4 - currentMaskingKeyIndex ?
                        4 - currentMaskingKeyIndex : (int) buffer.readableBytes();
                buffer.get(maskingKey,0, total);
                currentMaskingKeyIndex += total;
            } else {
                if (buffer.readableBytes() >= 4) {
                    buffer.get(maskingKey);
                    currentMaskingKeyIndex += 4;
                } else {
                    int total = (int) buffer.readableBytes();
                    buffer.get(maskingKey,0, total);
                    currentMaskingKeyIndex += total;
                }
            }
            return currentMaskingKeyIndex >= 4;
        }
    }

    /**
     * 非文本和二进制数据帧需要做不同的逻辑处理
     *
     * @return 特殊数据帧处理返回true
     */
    private boolean doHandleSpecialFrame(IoProcessor processor, WebSocketFrame frame) {
        if (frame.getOpcode() == WebSocketFrame.OPCODE_CLOSE) {
            processor.close(true);
            return true;
        }
        return false;
    }
}
