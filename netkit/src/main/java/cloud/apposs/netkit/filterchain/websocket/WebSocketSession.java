package cloud.apposs.netkit.filterchain.websocket;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.util.Encoder;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class WebSocketSession {
    public static final String CRLF = "\r\n";

    /**
     * IO处理器，一个客户端请求对应一个实例
     */
    private final IoProcessor processor;

    /**
     * 远程请求IP，即客户端IP
     */
    private final SocketAddress remoteAddr;

    /**
     * 请求HEADER
     */
    private final Map<String, String> headers = new HashMap<String, String>();

    /**
     * 请求传递参数
     */
    private final Map<String, String> parameters = new HashMap<String, String>();

    private String charset = "UTF-8";

    /**
     * 请求方法，有POST/GET/DELETE/PUT等
     */
    private String method;
    private String protocol;
    private String requestUri;
    private String remoteHost;

    public WebSocketSession(IoProcessor processor, SocketAddress remoteAddr) {
        this.processor = processor;
        this.remoteAddr = remoteAddr;
    }

    public SocketAddress getRemoteAddr() {
        return remoteAddr;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        if (requestUri != null) {
            requestUri = Encoder.decodeUrl(requestUri);
        }
        this.requestUri = requestUri;
    }

    public String getRemoteHost() {
        if (StrUtil.isEmpty(remoteHost)) {
            remoteHost = getHeader("host", true);
        }
        return remoteHost;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return getHeader(key, false);
    }

    public String getHeader(String key, boolean ignoreCase) {
        if (ignoreCase) {
            for (String k : headers.keySet()) {
                if (k.equalsIgnoreCase(key)) {
                    return headers.get(k);
                }
            }
        }
        return headers.get(key);
    }

    public boolean isHeaderContains(String key) {
        return isHeaderContains(key, false);
    }

    public boolean isHeaderContains(String key, boolean ignoreCase) {
        if (ignoreCase) {
            for (String k : headers.keySet()) {
                if (k.equalsIgnoreCase(key)) {
                    return true;
                }
            }
            return false;
        } else {
            return headers.containsKey(key);
        }
    }

    public void putHeader(String key, String value) {
        headers.put(key, value);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getParameter(String key) {
        String value = parameters.get(key);
        if (value != null) {
            return Encoder.decodeUrl(value);
        }
        return value;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void writeln(String message) throws IOException {
        write(message + CRLF, Charset.forName(charset), false);
    }

    public void write(String message) throws IOException {
        write(message, Charset.forName(charset), false);
    }

    public void writeln(String message, boolean flush) throws IOException {
        write(message + CRLF, Charset.forName(charset), flush);
    }

    public void write(byte[] buffer) throws IOException {
        write(buffer, WebSocketFrame.OPCODE_BINARY, false);
    }

    public void write(byte[] buffer, boolean flush) throws IOException {
        write(buffer, WebSocketFrame.OPCODE_BINARY, flush);
    }

    public void write(byte[] buffer, byte opcode, boolean flush) throws IOException {
        byte[] header = WebSocketUtil.generateServerHeader(buffer.length, opcode);
        processor.write(header);
        processor.write(buffer);
        if (flush) {
            processor.flush();
        }
    }

    /**
     * 响应字符串
     *
     * @param message 响应字符串
     * @param charset 字符编码
     * @param flush 主要服务于RxIo异步，
     *      因为RxIo的异步性，当前Server所在的EventLoop是其中一个线程，但RxIo中的EventLoop又是另外一个线程，
     *      所以不触发write写事件则当前Server的EventLoop线程是不会主动触发发送事件的
     */
    public void write(String message, Charset charset, boolean flush) throws IOException {
        byte[] buffer = message.getBytes(charset);
        byte[] header = WebSocketUtil.generateServerHeader(buffer.length, WebSocketFrame.OPCODE_TEXT);
        processor.write(header);
        processor.write(buffer);
        if (flush) {
            processor.flush();
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
        byte[] header = WebSocketUtil.generateServerHeader(buffer.readableBytes(), WebSocketFrame.OPCODE_BINARY);
        processor.write(header);
        processor.write(buffer);
        if (flush) {
            processor.flush();
        }
    }

    /**
     * 关闭会话
     */
    public void close(boolean immediately) {
        processor.close(immediately);
    }

    /**
     * 释放会话资源，一般在会话关闭或者异常产生时自动释放资源
     */
    public void release() {
        headers.clear();
        parameters.clear();
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(128);
        info.append("{");
        info.append("Remote: ").append(remoteAddr).append(", ");
        info.append("Method: ").append(method).append(", ");
        info.append("Uri: ").append(requestUri).append(", ");
        info.append("Host: ").append(getRemoteHost());
        info.append("}");
        return info.toString();
    }
}
