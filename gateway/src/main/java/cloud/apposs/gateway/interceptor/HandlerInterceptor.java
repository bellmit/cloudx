package cloud.apposs.gateway.interceptor;

import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.Param;

/**
 * WEB拦截器，对HTTP REQUEST请求进行拦截操作，每个IHandler对应一个拦截器实例，
 * 一般用于登录验证，XSS/CSRF安全检测等拦截操作中即可实现此接口
 */
public interface HandlerInterceptor {
	/**
	 * WEB容器启动时的拦截器初始化，只调用一次
	 */
	void init(Param arguments);
	
	/**
	 * 该方法在请求处理之前进行调用，
	 * 可实现登录/权限/自定义注解等拦截操作
	 * 
	 * @param  request    请求对象
     * @param  response   响应对象，像登录验证不通过则需要输出重新登录信息
     * @param  handler    业务方法处理器
     * @return true/false 通过拦截验证验证返回true
	 */
	boolean preHandle(HttpRequest request, HttpResponse response, IHandler handler) throws Exception;

	/**
	 * 整个请求处理完毕回调方法，无论请求逻辑处理有没有成功，
	 * 一般用于性能监控中在此记录结束时间并输出消耗时间，还可以进行一些资源清理
	 * 
	 * @param request   请求对象
     * @param response  响应对象，像登录验证不通过则需要输出重新登录信息
     * @param handler   业务方法处理器，异常产生时有可能为空，需要进行空值判断
	 * @param throwable 如果业务调用产生了异常，则该值不为空，即表示业务处理逻辑出现了问题
	 */
	void afterCompletion(HttpRequest request, HttpResponse response, IHandler handler, Throwable throwable);
	
	/**
	 * WEB容器关闭时的拦截器销毁，只调用一次
	 */
	void destory();
}
