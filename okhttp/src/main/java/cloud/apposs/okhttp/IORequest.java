package cloud.apposs.okhttp;

import cloud.apposs.netkit.rxio.io.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求包装器
 */
public final class IORequest {
    /**
     * 反向代理模式，有SOCKS5代理和HTTP代理
     */
    public static class ProxyMode {
        public static final int SOCKS = 0;
        public static final int HTTP = 1;
    }

    /**
     * HTTP请求URL
     */
    private String url;

    /**
     * 服务注册实例ID，若为空则不走反向代理负载均衡
     */
    private String serviceId;

    /**
     * 服务的请求KEY，Discovery负载均衡组件会通过此KEY来实现不同的负载均衡算法
     */
    private Object key;

    /**
     * 当前请求的代理模式
     */
    private int proxyMode = -1;

    /**
     * HTTP请求方法
     */
    private HttpMethod method = HttpMethod.GET;

    /**
     * HTTP表单数据
     */
    private FormEntity formEntity;

    /**
     * HTTP请求HEADERS数据
     */
    protected final Map<String, String> headers = new HashMap<String, String>();

    public static IORequest builder() {
        return new IORequest();
    }

    private IORequest()  {
    }

    public String url() {
        return url;
    }

    public IORequest url(String url) {
        this.url = url;
        return this;
    }

    public String serviceId() {
        return serviceId;
    }

    public IORequest serviceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public Object key() {
        return this.key;
    }

    public IORequest key(Object key) {
        this.key = key;
        return this;
    }

    public HttpMethod method() {
        return method;
    }

    public int proxyMode() {
        return this.proxyMode;
    }

    public IORequest proxyMode(int proxyMode) {
        this.proxyMode = proxyMode;
        return this;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public IORequest addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public IORequest addHeaders(Map<String, String> headers) {
        headers.putAll(headers);
        return this;
    }

    public FormEntity formEntity() {
        return formEntity;
    }

    public IORequest get(FormEntity formEntity) {
        return request(HttpMethod.GET, formEntity);
    }

    public IORequest put(FormEntity formEntity) {
        return request(HttpMethod.PUT, formEntity);
    }

    public IORequest post(FormEntity formEntity) {
        return request(HttpMethod.POST, formEntity);
    }

    public IORequest delete(FormEntity formEntity) {
        return request(HttpMethod.DELETE, formEntity);
    }

    public IORequest request(HttpMethod method, FormEntity formEntity) {
        this.method = method;
        this.formEntity = formEntity;
        return this;
    }
}
