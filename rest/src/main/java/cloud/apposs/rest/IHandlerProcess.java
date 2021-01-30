package cloud.apposs.rest;

import java.util.Map;

/**
 * 网络请求参数的处理接口
 */
public interface IHandlerProcess<R, P> {
    /**
     * 解析获取请求方法，例如POST/GET/DELETE/PUT等
     */
    String getRequestMethod(R request, P response);

    /**
     * 解析获取请求URL，例如www.aaa.com/usr中的usr
     */
    String getRequestPath(R request, P response);

    /**
     * 解析获取请求Host，例如www.aaa.com/usr中的www.aaa.com
     */
    String getRequestHost(R request, P response);

    /**
     * 处理Restful框架解析的请求参数，
     * 一般用于参数保存到上下文之中便于{@link cloud.apposs.rest.parameter.ParameterResolver}解析参数
     */
    void processVariable(R request, P response, Map<String, String> variables);

    /**
     * 解析获取Guard熔断参数解析服务
     */
    IGuardProcess<R, P> getGuardProcess();

    /**
     * 请求被标记为{@link cloud.apposs.rest.annotation.Async}时的回调
     */
    void markAsync(R request, P response);
}
