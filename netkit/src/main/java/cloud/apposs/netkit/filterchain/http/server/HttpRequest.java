package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.Encoder;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端请求数据包装
 */
public class HttpRequest {
    /**
     * 远程请求IP，即客户端IP
     */
    private final SocketAddress remoteAddr;

    /**
     * 请求HEADER
     */
    private final Map<String, String> headers = new HashMap<String, String>();

    /**
     * 表单字段数据，支持GET/POST/FORM-URL/FORM-DATA-FIELD
     */
    private final Map<String, String> parameters = new HashMap<String, String>();

    /**
     * 表单文件上传字段，数据结构为[FileName:FileItem]
     */
    private final Map<String, HttpFormFile> files = new HashMap<String, HttpFormFile>();

    /**
     * 当前会话请求存储的一些状态值
     */
    private final Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(1);

    /**
     * 表单JOSN数据
     */
    private final Param param = new Param();

    /**
     * 请求方法，有POST/GET/DELETE/PUT等
     */
    private String method;
    private String protocol;
    private String requestUrl;
    private String requestUri;
    private String remoteHost;

    /**
     * 解析完头部后的HTTP BODY字节码，
     * 主要是给下一个过滤器或者业务方进行BODY解码
     */
    private IoBuffer content;

    public HttpRequest(SocketAddress remoteAddr) {
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

    public String getSchema() {
        // 当前底层所有请求都是HTTP，
        // 如果想采用HTTPS可以在HttpServer中添加SSLFilter或者通过nginx反向代理来做
        return "http";
    }

    public String getRequestUrl() {
        if (StrUtil.isEmpty(requestUrl)) {
            String host = getRemoteHost();
            if (StrUtil.isEmpty(host)) {
                host = remoteAddr.toString();
            }
            requestUrl = getSchema() + "://" + host + requestUri;
        }
        return requestUrl;
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

    public void addFile(String key, HttpFormFile file) {
        files.put(key, file);
    }

    public Map<String, HttpFormFile> getFiles() {
        return files;
    }

    /**
     * 获取表单文件
     */
    public HttpFormFile getFile(String key) {
        return files.get(key);
    }

    public Param getParam() {
        return param;
    }

    public String getRemoteHost() {
        if (StrUtil.isEmpty(remoteHost)) {
            remoteHost = getHeader("host", true);
        }
        return remoteHost;
    }

    public IoBuffer getContent() {
        return content;
    }

    public void setContent(IoBuffer content) {
        this.content = content;
    }

    public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }

    public final Object getAttribute(Object key, Object defaultVal) {
        Object attr = attributes.get(key);
        if (attr == null && defaultVal != null) {
            attr = defaultVal;
            attributes.put(key, attr);
        }
        return attr;
    }

    public final Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }

    public final boolean hasAttribute(Object key) {
        return attributes.containsKey(key);
    }

    public void release() {
        requestUrl = null;
        headers.clear();
        parameters.clear();
        attributes.clear();
        param.clear();
        for (HttpFormFile file : files.values()) {
            try {
                file.delete();
            } catch (IOException ignore) {
            }
        }
        files.clear();
    }
}
