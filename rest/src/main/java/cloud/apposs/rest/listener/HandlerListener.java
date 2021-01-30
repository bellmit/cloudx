package cloud.apposs.rest.listener;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.RestConfig;

/**
 * {@link cloud.apposs.rest.Handler}监听器，一般全局单例
 */
public interface HandlerListener<R, P> {
    /**
     * WEB容器启动时的拦截器初始化，只调用一次
     */
    void initialize(RestConfig config);

    /**
     * 请求处理开始时的监听，当请求的URL获取不到匹配的Handler时不会进入此方法
     */
    void handlerStart(R request, P response, Handler handler);

    /**
     * 整个请求处理完毕时的监听，无论请求逻辑处理有没有成功，
     * 一般用于性能监控中在此记录结束时间并输出消耗时间
     *
     * @param handler 请求处理器，但当Handler不存在时为空
     * @param result Handler处理请求返回结果，如果出现异常则此参数统计意义不大
     * @param t 如果业务调用产生了异常，则该值不为空，即表示业务处理逻辑出现了问题
     */
    void handlerComplete(R request, P response, Handler handler, Object result, Throwable t);
}
