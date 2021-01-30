package cloud.apposs.rest.plugin;

import cloud.apposs.rest.RestConfig;

/**
 * 服务初始化，在Web容器启动时初始化，在容器销毁时注销，
 * 一些重资源例如数据库池或者其他资源池需要一开始初始化时则实现此类
 */
public interface Plugin<R, Q> {
	/**
	 * 在Web容器启动时初始化
	 */
	void initialize(RestConfig config);
	
	/**
	 * 在Web容器关闭时的注销
	 */
	void destroy();
}
