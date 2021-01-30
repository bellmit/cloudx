package cloud.apposs.rest.interceptor;

import cloud.apposs.rest.ApplicationContext;
import cloud.apposs.rest.Handler;

public class HandlerInterceptorAdapter<R, P> implements HandlerInterceptor<R, P> {
	@Override
	public void initialize(ApplicationContext context) {
	}

	@Override
	public boolean preHandle(R request, P response, Handler handler) throws Exception {
		return true;
	}
	
	@Override
	public void postHandle(R request, P response, Handler handler) throws Exception {
	}

	@Override
	public void afterCompletion(R request, P response, Handler handler, Object result, Throwable throwable) {
	}

	@Override
	public void destory() {
	}
}
