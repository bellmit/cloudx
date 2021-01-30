package cloud.apposs.bootor.sample.inceptor;

import cloud.apposs.bootor.interceptor.BooterInterceptorAdaptor;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

public class LogInterceptor extends BooterInterceptorAdaptor {
    private static final String LOG_INTERCEPTOR_ATTR = "_AttrLogInterceptor";

    @Override
    public boolean preHandle(HttpRequest request, HttpResponse response, Handler handler) throws Exception {
        Logger.info("request log interceptor in:" + handler);
        request.setAttribute(LOG_INTERCEPTOR_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpRequest request, HttpResponse response, Handler handler, Object result, Throwable throwable) {
        Object time = request.getAttribute(LOG_INTERCEPTOR_ATTR);
        if (time == null) {
            return;
        }
        long startTime = (long) time;
        Logger.info("request log interceptor finish:" + (System.currentTimeMillis() - startTime));
    }
}
