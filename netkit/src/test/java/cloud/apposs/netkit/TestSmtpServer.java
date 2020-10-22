package cloud.apposs.netkit;

import cloud.apposs.logger.Level;
import cloud.apposs.netkit.buffer.Allocator;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.buffer.SimpleAllocator;
import cloud.apposs.netkit.buffer.ZeroCopyAllocator;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.executor.ThreadPool;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolFilter;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolHandler;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolType;
import cloud.apposs.netkit.filterchain.line.TextLineFilter;
import cloud.apposs.netkit.filterchain.logging.LoggingFilter;
import cloud.apposs.netkit.filterchain.ssl.SslFilter;
import cloud.apposs.netkit.server.ServerConfig;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;
import cloud.apposs.netkit.server.TcpServer;
import cloud.apposs.util.SysUtil;

import java.util.ArrayList;
import java.util.List;

public class TestSmtpServer {
    public static final String DIR = System.getProperty("user.dir") + "/res/";
    public static final String SSL_KEYSTORE_PATH = DIR +  "sslsocket.keystore";
    public static final String SSL_KEYSTORE_PASSWORD = "fai@508";
    public static final boolean USE_ZERO_COPY = true;
    public static final boolean USE_THREAD = true;
    public static final boolean USE_LOG = false;

    public static void main(String[] args) throws Exception {
        Allocator allocator = null;
        if (USE_ZERO_COPY) {
            allocator = new ZeroCopyAllocator(1024 * 1024, "D://Tmp/");
        } else {
            allocator = new SimpleAllocator();
        }
        IoAllocator.setAllocator(allocator);

        // NORMAL SMTP
        ServerConfig config = new ServerConfig();
        config.setHost("0.0.0.0");
        config.setPort(25);
        config.setNumOfGroup(4);
        config.setRecvTimeout(600000);
        TcpServer smtpSvr = new TcpServer(config);
        if (USE_LOG) {
            smtpSvr.getFilterChain().addFilter(new LoggingFilter(Level.INFO));
        }
        smtpSvr.getFilterChain().addFilter(new TextLineFilter("\r\n"));
        if (USE_THREAD) {
            smtpSvr.getFilterChain().addFilter(new ThreadPoolFilter(new MyThreadPoolHandler("NormalPool")));
        }
        smtpSvr.setHandler(new SimpleSmtpHandler());
        smtpSvr.start();

        // SSL SMTP
        ServerConfig sslCfg = new ServerConfig();
        sslCfg.setHost("0.0.0.0");
        sslCfg.setPort(465);
        sslCfg.setNumOfGroup(4);
        sslCfg.setRecvTimeout(600000);
        TcpServer sslSvr = new TcpServer(sslCfg);
        IoFilter filter = new SslFilter(SSL_KEYSTORE_PATH, SSL_KEYSTORE_PASSWORD, false, true);
        sslSvr.getFilterChain().addFilter(filter);
        if (USE_LOG) {
            sslSvr.getFilterChain().addFilter(new LoggingFilter(Level.INFO));
        }
        sslSvr.getFilterChain().addFilter(new TextLineFilter("\r\n"));
        if (USE_THREAD) {
            sslSvr.getFilterChain().addFilter(new ThreadPoolFilter(new MyThreadPoolHandler("SSLPool")));
        }
        sslSvr.setHandler(new SimpleSmtpHandler());
        sslSvr.start();
    }

    static class SimpleSmtpHandler extends ServerHandlerAdaptor {
        @Override
        public byte[] getWelcome() {
            return "220 Welcome To Smtp Server\r\n".getBytes();
        }

        @Override
        public void channelRead(ServerHandlerContext context, Object message)
                throws Exception {
            IoBuffer buf = (IoBuffer) message;
            Boolean isData = (Boolean) context.getAttribute("data", false);
            if (isData) {
                TextLineFilter.Context tlfCtx = (TextLineFilter.Context) context.getAttribute(TextLineFilter.FILTER_CONTEXT);
                if (tlfCtx != null) {
                    tlfCtx.setEndString("\r\n");
                }
                context.write("250 Ok\r\n".getBytes());
                context.setAttribute("data", false);
                String flushFile = "D://Tmp/mail" + SysUtil.random() + ".eml";
                System.out.println("RecvBuf=" + buf + ";FlushLen=" + buf.flush(flushFile));
            } else {
                final String recvString = buf.string();
                String command = getCmdValueString(recvString);
                if (command.toUpperCase().equals("EHLO")) {
                    context.write("250 DSN\r\n".getBytes());
                } else if (command.toUpperCase().equals("MAIL")) {
                    context.write("250 Ok\r\n".getBytes());
                } else if (command.toUpperCase().equals("RCPT")) {
                    context.write("250 Ok\r\n".getBytes());
                } else if (command.toUpperCase().equals("DATA")) {
                    context.setAttribute("data", true);
                    context.write("354 End data with <CR><LF>.<CR><LF>\r\n".getBytes());

                    TextLineFilter.Context tlfCtx = (TextLineFilter.Context) context.getAttribute(TextLineFilter.FILTER_CONTEXT);
                    if (tlfCtx != null) {
                        tlfCtx.setEndString("\r\n.\r\n");
                    }
                } else if (command.toUpperCase().equals("QUIT")) {
                    context.write("221 Bye\r\n".getBytes());
                    context.close(false);
                }
                System.out.println("Thread:" + Thread.currentThread() + ";" + recvString);
            }
        }

        @Override
        public void channelError(ServerHandlerContext context, Throwable cause) {
            cause.printStackTrace();
        }

        private static String getCmdValueString(String cmd) {
            String[] strs = cmd.trim().split("\\s+");
            return strs[0].trim();
        }
    }

    private static class MyThreadPoolHandler implements ThreadPoolHandler {
        private final String threadName;

        public MyThreadPoolHandler(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public List<ThreadPool> createPoolGroups() {
            List<ThreadPool> pools = new ArrayList<ThreadPool>();
            pools.add(new ThreadPool(threadName, 2));
            return pools;
        }

        @Override
        public ThreadPoolType getThreadPoolType(Object message) {
            return new ThreadPoolType(threadName, null);
        }
    }
}
