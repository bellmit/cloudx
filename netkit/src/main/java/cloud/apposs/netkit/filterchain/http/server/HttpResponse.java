package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.server.ServerHandlerContext;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.MediaType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 服务端数据响应包装
 */
public class HttpResponse {
    public static final String CRLF = "\r\n";

    private final ServerHandlerContext context;

    private final Map<String, String> headers = new HashMap<String, String>();

    private String version = "HTTP/1.1";
    private HttpStatus status = HttpStatus.HTTP_STATUS_200;
    private String server = HttpConstants.HTTP_SERVER_NAME;
    private String charset = "UTF-8";

    public HttpResponse(ServerHandlerContext context) {
        this.context = context;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void putHeader(String key, String value) {
        headers.put(key, value);
    }

    public void writeln(String message) throws IOException {
        write(message + CRLF, false);
    }

    public void write(String message) throws IOException {
        write(message, false);
    }

    public void writeln(String message, boolean flush) throws IOException {
        write(message + CRLF, flush);
    }

    public void write(byte[] message) throws IOException {
        write(message, false);
    }

    public void write(byte[] message, boolean flush) throws IOException {
        String header = doGenerateHeader(message.length);
        context.write(header);
        context.write(message);
        if (flush) {
            context.flush();
        }
    }

    /**
     * 响应字符串
     *
     * @param message 响应字符串
     * @param flush 主要服务于RxIo异步，
     *      因为RxIo的异步性，当前Server所在的EventLoop是其中一个线程，但RxIo中的EventLoop又是另外一个线程，
     *      所以不触发write写事件则当前Server的EventLoop线程是不会主动触发发送事件的
     */
    public void write(String message, boolean flush) throws IOException {
        String header = doGenerateHeader(message.getBytes(Charset.forName(charset)).length);
        context.write(header);
        context.write(message);
        if (flush) {
            context.flush();
        }
    }

    /**
     * 响应字节码，底层可用零拷贝来传输数据
     *
     * @param buffer 字节码数据
     * @param flush 主要服务于RxIo异步，
     *      因为RxIo的异步性，当前Server所在的EventLoop是其中一个线程，但RxIo中的EventLoop又是另外一个线程，
     *      所以不触发write写事件则当前Server的EventLoop线程是不会主动触发发送事件的
     */
    public void write(IoBuffer buffer, boolean flush) throws IOException {
        String header = doGenerateHeader(buffer.readableBytes());
        context.write(header);
        context.write(buffer);
        if (flush) {
            context.flush();
        }
    }

    public void flush() throws IOException {
        String header = doGenerateHeader(0);
        context.write(header);
        context.flush();
    }

    public String getContentType() {
        return headers.get("Content-Type");
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    public void setContentType(MediaType mediaType) {
        headers.put("Content-Type", mediaType.getType());
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void close(boolean immediately) {
        context.close(immediately);
    }

    private String doGenerateHeader(long contentLength) {
        StringBuilder response = new StringBuilder(128);
        response.append(version)
                .append(" ")
                .append(status.getCode())
                .append(" ")
                .append(status.getDescription())
                .append(CRLF);
        response.append("Server: ").append(server).append(CRLF);
        response.append("Content-Length: ").append(contentLength).append(CRLF);
        if (!checkHeader("content-type")) {
            headers.put("Content-Type", "text/plain;charset=" + charset);
        }
        for (Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            response.append(key).append(": ").append(value).append(CRLF);
        }
        response.append(CRLF);
        return response.toString();
    }

    public boolean checkHeader(String key) {
        for (String k : headers.keySet()) {
            if (k.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }
}
