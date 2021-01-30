package cloud.apposs.gateway.handler;

import cloud.apposs.gateway.interceptor.HandlerInterceptor;
import cloud.apposs.gateway.interceptor.HandlerInterceptorSupport;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.util.Param;

import java.util.Map;

/**
 * HTTP请求处理服务，支持反向HTTP/SOCKS代理，
 * 每个微服务链接对应一个单例Handler，即一个location xxx {...}配置块对应一个Handler+FilterChain
 */
public interface IHandler {
    /**
     * 初始化当前HTTP处理服务，只在网关启动或者配置重载时初始化一次
     */
    void initialize(Param options) throws Exception;

    /**
     * 处理HTTP请求，根据处理返回标志，上层ApplicationHandler会根据结果决定是否进行最后拦截器触发，
     * 注意如果返回是异步即为true，则底层handler需要自己触发拦截器，便于保证拦截器生命周期完整
     *
     * @return 异步处理结果
     */
    RxIo<?> handle(HttpRequest request, HttpResponse response) throws Exception;

    /**
     * 添加IHandler请求处理拦截器
     */
    void addInterceptor(HandlerInterceptor interceptor);

    /**
     * location匹配的主机名
     */
    String getHost();
    void setHost(String host);

    /**
     * location匹配的请求路径
     */
    String getPath();
    void setPath(String path);

    /**
     * location路径是否为正则
     */
    boolean isPattern();
    void setPattern(boolean pattern);

    /**
     * 文本输出CONTENT-TYPE
     */
    String getContentType();
    void setContentType(String contentType);

    /**
     * 服务处理编码
     */
    String getCharset();
    void setCharset(String charset);

    /**
     * 获取当前Handler的拦截器列表
     */
    HandlerInterceptorSupport getInterceptorSupport();

    /**
     * 反向代理自定义输出Header列表
     */
    void addHeaders(Map<String, String> headers);
    Map<String, String> addHeaders();

    /**
     * 反向代理自定义代理请求Header列表
     */
    void proxyHeaders(Map<String, String> headers);
    Map<String, String> proxyHeaders();

    /**
     * 关闭处理器，释放资源
     */
    void close();
}
