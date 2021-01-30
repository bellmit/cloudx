package cloud.apposs.rest.listener;

import cloud.apposs.rest.RestConfig;

import java.util.EventListener;

/**
 * 框架监听服务，主要服务于框架启动时初始化
 */
public interface ApplicationListener extends EventListener {
	/**
	 * 在Web容器启动时初始化
	 */
	void initialize(RestConfig config);
	
	/**
	 * 在Web容器关闭时销毁
	 */
	void destroy();
}
