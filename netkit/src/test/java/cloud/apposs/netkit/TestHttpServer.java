package cloud.apposs.netkit;

import cloud.apposs.netkit.filterchain.http.server.HttpFormFile;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.server.http.HttpHandler;
import cloud.apposs.netkit.server.http.HttpServer;
import cloud.apposs.netkit.server.http.HttpServerConfig;
import cloud.apposs.netkit.server.http.HttpSession;

import java.io.File;

public class TestHttpServer {
    public static void main(String[] args) throws Exception {
        HttpServerConfig config = new HttpServerConfig();
        config.setPort(8880);
        config.setDirectory("C:\\upload\\tmp");
        HttpServer server = new HttpServer(config);
        server.setHandler(new HttpSimple());
        server.start();
    }

    static class HttpSimple extends HttpHandler {
        @Override
        public void service(HttpSession session) throws Exception {
            String content = "Hello Html Server";
            HttpResponse response = session.getResponse();
            response.writeln(content);
        }
    }

    static class HttpFull extends HttpHandler {
        @Override
        public void service(HttpSession session) throws Exception {
            String content = "Html Server";
            System.out.println("HttpBody " + Thread.currentThread());
            HttpRequest request = session.getRequest();
            HttpResponse response = session.getResponse();
            System.out.println(request.getProtocol());
            System.out.println(request.getRemoteAddr());
            System.out.println(request.getRemoteHost());
            System.out.println(request.getHeaders());
            System.out.println(request.getParameters());
            response.writeln(content);
        }
    }

    /**
     * 测试大文件上传，底层采用临时文件存储以减少JVM压力
     */
    static class HttpFile extends HttpHandler {
        @Override
        public void service(HttpSession session) throws Exception {
            String content = "Upload OK";
            HttpRequest request = session.getRequest();
            HttpFormFile file = request.getFile("file_name");
            if (file != null) {
                System.out.println("HttpFile Save " + Thread.currentThread());
                file.transfer(new File("C://upload/" + file.getFilename()));
            } else {
                System.out.println("HttpFile Url " + Thread.currentThread());
            }
            HttpResponse response = session.getResponse();
            response.writeln(content);
        }
    }

    /**
     * 测试HTTP参数请求
     * curl -i -X POST -H "Content-type:application/json" -d '{"id":10899,"name":"product1"}' http://172.17.2.11:8880/
     */
    static class HttpParam extends HttpHandler {
        @Override
        public void service(HttpSession session) throws Exception {
            String content = "Hello Json Server";
            HttpRequest request = session.getRequest();
            HttpResponse response = session.getResponse();
            System.out.println(request.getParam());
            response.writeln(content);
        }
    }
}
