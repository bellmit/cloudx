package cloud.apposs.netkit.rxio.io.http;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventSocketChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.http.client.HttpFilter;
import cloud.apposs.netkit.filterchain.socks.SocksFilter;
import cloud.apposs.netkit.filterchain.ssl.SslFilter;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * HTTP客户端异步请求，每个请求对应每个IoHttp实例，
 * 底层采用的是EventLoop进行网络事件触发
 */
public class IoHttp extends AbstractIoProcessor {
    public static final String HTTP_PROTOCL_1 = "HTTP/1.1";
    public static final int HTTP_PORT_NORMAL = 80;
    public static final int HTTP_PORT_SSL = 443;

    public static final String CRLF = "\r\n";
    public static final String CRLFCRLF = "\r\n\r\n";

    /**
     * 当前客户端网络句柄
     */
    private EventSocketChannel channel;

    protected final URI uri;

    /**
     * 当前的HTTP请求方法
     */
    protected final HttpMethod method;

    /**
     * HTTP代理请求，有SOCKS5/HTTP
     */
    protected final Proxy proxy;

    /**
     * HTTP USER_AGENT
     */
    protected String userAgent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 IoHttp/1.0.0";

    /**
     * HTTP请求HEADERS数据
     */
    protected final Map<String, String> headers = new HashMap<String, String>();

    /**
     * HTTP表单数据
     */
    protected HttpForm form;

    public IoHttp(String url) throws Exception {
        this(url, HttpMethod.GET, null);
    }

    public IoHttp(String url, HttpMethod method) throws Exception {
        this(url, method, null);
    }

    public IoHttp(String url, Proxy proxy) throws Exception {
        this(url, HttpMethod.GET, proxy);
    }

    public IoHttp(String url, HttpMethod method, Proxy proxy) throws Exception {
        SysUtil.checkNotNull(url, "url");
        SysUtil.checkNotNull(method, "method");

        String tmpUtl = url.toLowerCase();
        if (!tmpUtl.startsWith("http://") && !tmpUtl.startsWith("https://")) {
            url = "http://" + url;
        }
        this.uri = URI.create(url);
        this.proxy = proxy;
        this.method = method;

        if (proxy != null) {
            chain.add(new SocksFilter());
        }
        if (uri.getScheme().equals("https")) {
            chain.add(new SslFilter(true, true));
        }
        chain.add(new HttpFilter(this, url, method));
    }

    @Override
    public void channelConnect() throws IOException {
        IoBuffer[] buffers = doWrapRequest();
        write(buffers);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(Object message) throws Exception {
        try {
            if (!(message instanceof HttpAnswer)) {
                return;
            }
            HttpAnswer response = (HttpAnswer) message;

            // 该代码主要服务响应式异步调用
            if (context instanceof IoSubscriber) {
                IoSubscriber subscribe = (IoSubscriber) getContext();
                subscribe.onNext(response);
            }
        } finally {
            close(true);
        }
    }

    @Override
    public void channelReadEof(Object message) throws Exception {
        // 该代码主要服务响应式异步调用
        if (context instanceof IoSubscriber) {
            IoSubscriber subscribe = (IoSubscriber) getContext();
            subscribe.onError(new IOException("remote address '" + uri + "' colsed"));
        }
    }

    @Override
    public void channelError(Throwable cause) {
        // 该代码主要服务响应式异步调用
        if (context instanceof IoSubscriber) {
            IoSubscriber subscribe = (IoSubscriber) getContext();
            subscribe.onError(new IOException("remote address '" + uri + "' transmission fail", cause));
        }
    }

    @Override
    public EventChannel getChannel() {
        return channel;
    }

    @Override
    public SelectionKey doRegister(Selector selector) throws IOException {
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null) {
            throw new IllegalArgumentException("uri '" + uri + "' hostname can't be null");
        }

        String scheme = uri.getScheme();
        if (port < 0) {
            port = HTTP_PORT_NORMAL;
            if ("https".equals(scheme)) {
                port = HTTP_PORT_SSL;
            }
        }
        InetSocketAddress addr = new InetSocketAddress(host, port);

        // 判断是否使用代理，用代理则需要重写连接地址
        if (proxy != null) {
            SocksFilter socks = (SocksFilter) chain.get(SocksFilter.class);
            if (socks != null) {
                addr = socks.channelProxy(this, proxy, addr);
            }
        }

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setTcpNoDelay(true);

        channel = new EventSocketChannel(socketChannel);
        channel.connect(addr);
        return channel.register(selector, SelectionKey.OP_CONNECT);
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public IoHttp addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public IoHttp addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public void setForm(HttpForm form) {
        this.form = form;
    }

    /**
     * GET/POST请求的Header头格式是不一样的，由各不同的请求来包装
     */
    protected IoBuffer[] doWrapRequest() throws IOException {
        StringBuilder request = new StringBuilder(128);
        String path = uri.getPath();
        if (StrUtil.isEmpty(path)) {
            path = "/";
        }
        String query = uri.getRawQuery();
        if (!StrUtil.isEmpty(query)) {
            path += "?" + query;
        }
        request.append(method).append(" " + path + " ");
        request.append(HTTP_PROTOCL_1 + CRLF);
        request.append("User-Agent: " + userAgent + CRLF);
        // 生成Header头
        if (!headers.containsKey("Host")) {
            headers.put("Host", uri.getHost());
        }
        if (!headers.containsKey("Accept")) {
            headers.put("Accept", "*/*");
        }
        Set<Entry<String, String>> headerList = headers.entrySet();
        int total = headerList.size();
        int current = 0;
        for (Entry<String, String> keyVal : headerList) {
            request.append(keyVal.getKey() + ": " + keyVal.getValue() + CRLF);
            if (++current >= total) {
                request.append(CRLF);
            }
        }
        IoBuffer[] buffers = new IoBuffer[1];
        buffers[0] = ByteBuf.wrap(request.toString(), "utf-8");
        return buffers;
    }

    @Override
    public String toString() {
        return method + " " + uri + " " + HTTP_PROTOCL_1;
    }
}
