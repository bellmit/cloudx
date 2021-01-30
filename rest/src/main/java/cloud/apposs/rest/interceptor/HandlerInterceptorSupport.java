package cloud.apposs.rest.interceptor;

import cloud.apposs.rest.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HandlerInterceptor}管理器
 */
public class HandlerInterceptorSupport<R, P> {
    /**
     * 创建一个拦截器列表（用于存放拦截器实例）
     */
    private List<HandlerInterceptor<R, P>> interceptorList;

    public HandlerInterceptorSupport() {
        this.interceptorList = new ArrayList<HandlerInterceptor<R, P>>();
    }

    public void addInterceptor(HandlerInterceptor<R, P> interceptor) {
        interceptorList.add(interceptor);
    }

    public void removeInterceptor(HandlerInterceptor<R, P> interceptor) {
        interceptorList.remove(interceptor);
    }

    /**
     * 请求开始时从第一个拦截器开始拦截
     */
    public boolean preAction(R request, P response, Handler handler) throws Exception {
        for (int i = 0; i < interceptorList.size(); i++) {
            HandlerInterceptor<R, P> interceptor = interceptorList.get(i);
            if (!interceptor.preHandle(request, response, handler)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 请求<b>成功</b>结束时从最后一个拦截器开始拦截
     */
    public void postAction(R request, P response, Handler handler) throws Exception {
        for (int i = interceptorList.size() - 1; i >= 0; i--) {
            HandlerInterceptor<R, P> interceptor = interceptorList.get(i);
            interceptor.postHandle(request, response, handler);
        }
    }

    /**
     * 整个请求处理完毕回调方法，无论请求逻辑处理<b>有没有成功</b>，
     * 一般用于性能监控中在此记录结束时间并输出消耗时间，还可以进行一些资源清理
     */
    public void afterCompletion(R request, P response, Handler handler, Object result) {
        afterCompletion(request, response, handler, result, null);
    }

    /**
     * 请求结束时，无论有业务方逻辑处理有没有成功，从最后一个拦截器开始拦截
     */
    public void afterCompletion(R request, P response, Handler handler, Object result, Throwable throwable) {
        for (int i = interceptorList.size() - 1; i >= 0; i--) {
            HandlerInterceptor<R, P> interceptor = interceptorList.get(i);
            interceptor.afterCompletion(request, response, handler, result, throwable);
        }
    }

    public void destroy() {
        for (int i = 0; i < interceptorList.size(); i++) {
            HandlerInterceptor<R, P> interceptor = interceptorList.get(i);
            interceptor.destory();
        }
    }
}
