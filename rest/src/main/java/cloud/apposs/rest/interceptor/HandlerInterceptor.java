package cloud.apposs.rest.interceptor;

import cloud.apposs.rest.ApplicationContext;
import cloud.apposs.rest.Handler;

/**
 * WEB拦截器，对HTTP REQUEST请求进行拦截操作，在全局WEB中只有一个实例，
 * 一般用于登录验证，XSS/CSRF安全检测等拦截操作中即可实现此接口或者自定义注解逻辑解析，
 * 注意和Handler不同，此拦截器是采用同步方式实现
 */
public interface HandlerInterceptor<R, P> {
    /**
     * WEB容器启动时的拦截器初始化，只调用一次
     *
     * @param context 框架容器上下文，拦截器可通过此参数来获取IOC实现类，实现对框架无侵入的开发
     */
    void initialize(ApplicationContext context);

    /**
     * 该方法在请求处理之前进行调用，
     * 可实现登录/权限/自定义注解等拦截操作
     *
     * @param request  请求对象
     * @param response 响应对象，像登录验证不通过则需要输出重新登录信息
     * @param handler  业务方法处理器
     * @return true/false 通过拦截验证验证返回true
     */
    boolean preHandle(R request, P response, Handler handler) throws Exception;

    /**
     * 该方法在请求成功处理之后进行调用，如果业务逻辑处理有异常产生该方法不会调用
     */
    void postHandle(R request, P response, Handler handler) throws Exception;

    /**
     * 整个请求处理完毕回调方法，无论请求逻辑处理有没有成功，
     * 一般用于性能监控中在此记录结束时间并输出消耗时间，还可以进行一些资源清理
     *
     * @param request   请求对象
     * @param response  响应对象，像登录验证不通过则需要输出重新登录信息
     * @param handler   业务方法处理器，异常产生时有可能为空，需要进行空值判断
     * @param result    业务响应值，有可能也是业务产生异常后生成的异常值
     * @param throwable 如果业务调用产生了异常，则该值不为空
     */
    void afterCompletion(R request, P response, Handler handler, Object result, Throwable throwable);

    /**
     * WEB容器关闭时的拦截器销毁，只调用一次
     */
    void destory();
}
