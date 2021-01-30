package cloud.apposs.gateway.handler;

import cloud.apposs.gateway.GatewayConstants;
import cloud.apposs.gateway.interceptor.HandlerInterceptor;
import cloud.apposs.gateway.interceptor.HandlerInterceptorSupport;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHandler implements IHandler {
    /**
     * 请求的匹配主机
     */
    protected String host;

    /**
     * 请求的匹配路径
     */
    protected String path;

    /**
     * 方法请求路径是否是正则表达式
     */
    protected boolean pattern = false;

    /**
     * 响应输出CONTENT-TYPE
     */
    protected String contentType = GatewayConstants.DEFAULT_CONTENT_TYPE;

    /**
     * 输出响应编码
     */
    protected String charset = GatewayConstants.DEFAULT_CHARSET;

    /**
     * 拦截器管理
     */
    protected HandlerInterceptorSupport interceptorSupport = new HandlerInterceptorSupport();

    /**
     * 反向代理自定义输出Header列表
     */
    protected final Map<String, String> addHeaders = new HashMap<String, String>();

    /**
     * 反向代理自定义代理请求Header列表
     */
    protected final Map<String, String> proxyHeaders = new HashMap<String, String>();

    @Override
    public void addInterceptor(HandlerInterceptor interceptor) {
        interceptorSupport.addInterceptor(interceptor);
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean isPattern() {
        return pattern;
    }

    @Override
    public void setPattern(boolean pattern) {
        this.pattern = pattern;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public HandlerInterceptorSupport getInterceptorSupport() {
        return interceptorSupport;
    }

    @Override
    public void addHeaders(Map<String, String> headers) {
        addHeaders.putAll(headers);
    }

    @Override
    public Map<String, String> addHeaders() {
        return addHeaders;
    }

    @Override
    public void proxyHeaders(Map<String, String> headers) {
        proxyHeaders.putAll(headers);
    }

    @Override
    public Map<String, String> proxyHeaders() {
        return proxyHeaders;
    }

    @Override
    public void close() {
    }
}
