package cloud.apposs.rest.view;

import cloud.apposs.rest.RestConfig;

/**
 * 视图解析渲染器，整个Web生命周期是单例
 */
public interface ViewResolver<R, P> {
    /**
     * 配置视图
     */
    ViewResolver build(RestConfig config);

    /**
     * 指定的返回结果该视图解析器是否支持，
     * 不同的视图解析器解析不同的Action类方法返回值
     */
    boolean supports(R request, P response, Object result);

    /**
     * 渲染视图
     *
     * @param request  页面请求
     * @param response 页面响应
     * @param result   视图结果，保存业务方面处理逻辑处理，渲染视图即是对该结果进行不同的格式输出
     * @param flush    是否立即刷出响应数据，服务于是RXIO异步响应式输出
     * @throws Exception 处理异常，可由自定义的{@link cloud.apposs.rest.WebExceptionResolver}拦截处理
     */
    void render(R request, P response, Object result, boolean flush) throws Exception;
}
