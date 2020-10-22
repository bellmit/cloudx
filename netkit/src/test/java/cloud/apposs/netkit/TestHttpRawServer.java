package cloud.apposs.netkit;

import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.line.TextLineFilter;
import cloud.apposs.netkit.rxio.IoAction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.io.http.IoHttp;
import cloud.apposs.netkit.server.ServerConfig;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;
import cloud.apposs.netkit.server.TcpServer;

public class TestHttpRawServer {
    public static void main(String[] args) throws Exception {
        ServerConfig config = new ServerConfig();
        config.setHost("0.0.0.0");
        config.setPort(8882);
        config.setNumOfGroup(16);
        TcpServer server = new TcpServer(config);
        server.getFilterChain().addFilter(new TextLineFilter("\r\n\r\n"));
        server.setHandler(new HttpBody());
        server.start();
    }

    /**
     * HTTP原始字节测试，主要是为了测试不用底层HTTP协议解析框架时的性能
     */
    static class HttpBody extends ServerHandlerAdaptor {
        @Override
        public void channelRead(final ServerHandlerContext context,
                                final Object message) throws Exception {
            System.out.println("HttpBody " + Thread.currentThread());
            StringBuilder response = new StringBuilder(1024);
            String content = "<html><head><title>Html Server</title></head><body>Hello World</body></html>";
            response.append("HTTP/1.1 200 OK");
            response.append("Server: bfe/1.0.8.18");
            response.append("Date: Wed, 09 May 2018 16:07:59 GMT");
            response.append("Content-Type: text/html");
            response.append("Content-Length: " + content.length());
            response.append("Last-Modified: Mon, 23 Jan 2017 13:28:37 GMT");
            response.append("ETag: 58860505-94d");
            response.append("Pragma: no-cache");
            response.append("Set-Cookie: BDORZ=27315; max-age=86400; domain=.baidu.com; path=/");
            response.append("Accept-Ranges: bytes\r\n\r\n");
            response.append(content);
            context.write(response.toString());
        }

        @Override
        public void channelSend(ServerHandlerContext context, WriteRequest request) {
            context.close(true);
        }
    }

    static class HttpHeader extends ServerHandlerAdaptor {
        @Override
        public void channelRead(final ServerHandlerContext context,
                                final Object message) throws Exception {
            System.out.println(Thread.currentThread());
            StringBuilder response = new StringBuilder(1024);
            response.append("HTTP/1.1 200 OK\r\n");
            response.append("Server: nginx\r\n");
            response.append("Date: Thu, 10 May 2018 02:55:20 GMT\r\n");
            response.append("Content-Type: application/octet-stream\r\n");
            response.append("Content-Length: 0\r\n");
            response.append("Connection: keep-alive\r\n\r\n");
            context.write(response.toString());
        }

        @Override
        public void channelSend(ServerHandlerContext context, WriteRequest request) {
            context.close(true);
        }
    }

    static class HttpSlow extends ServerHandlerAdaptor {
        @Override
        public void channelRead(final ServerHandlerContext context,
                                final Object message) throws Exception {
            System.out.println(Thread.currentThread());

            final EventLoopGroup group = context.getLoopGroup();
            // 异步通过http获取信息
            RxIo<HttpAnswer> request = RxIo.create(group, new IoHttp("http://itil.aaa.cn/slow.jsp"));
            request.subscribe(new IoAction<HttpAnswer>() {
                @Override
                public void call(HttpAnswer response) throws Exception {
                    context.write(response.getContent());
                }
            }).start();
        }

        @Override
        public void channelSend(ServerHandlerContext context, WriteRequest request) {
            context.close(true);
        }
    }
}
