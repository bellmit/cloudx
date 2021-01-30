package cloud.apposs.gateway.interceptor;

import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.Param;

public class HandlerInterceptorAdapter implements HandlerInterceptor {
	@Override
	public void init(Param arguments) {
	}

	@Override
	public boolean preHandle(HttpRequest request, HttpResponse response, IHandler handler) throws Exception {
		return true;
	}

	@Override
	public void afterCompletion(HttpRequest request, HttpResponse response, IHandler handler, Throwable throwable) {
	}

	@Override
	public void destory() {
	}
}
