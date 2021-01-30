package cloud.apposs.rest.listener;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.RestConfig;

public class HandlerListenerAdapter<R, P> implements HandlerListener<R, P> {
    @Override
    public void initialize(RestConfig config) {
    }

    @Override
    public void handlerStart(R request, P response, Handler handler) {
        // 因为Handler是单例，在多线程下有数据污染，需要用ThreadLocal或者request.setAttribute来做变量隔离
    }

    @Override
    public void handlerComplete(R request, P response, Handler handler, Object result, Throwable t) {
    }
}
