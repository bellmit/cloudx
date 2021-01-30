package cloud.apposs.rest.auth;

import java.util.Map;

/**
 * 用户会话权限实现，全局单例，业务方只需要实现此接口即可实现用户权限管理，会话认证核心接口，
 * 实现读取权限配置和存储用户会话信息可以有以下方式：
 * 1、从文件读取的实现方式
 * 2、从数据库读取的实现方式
 * 3、从RPC服务读取的实现方式
 */
public interface Authentication<R, P> {
	/**
	 * 加载获取所有的用户权限列表，
	 * 可从配置文件加载，也可从数据库加载，也可以直接在代码定义好，
	 * 执行时期：在系统启动时
	 */
	Map<String, AuthPermission> getAuthPermissionList();

	/**
	 * 获取会话ID，可通过Cookie或者Url获取用户登录后生成的会话ID
	 * 执行时期：在用户登录后
	 */
	String getSessionId(R request);

	/**
	 * 获取会话对应用户，
	 * 存储会话用户信息可直接存储于内存或者Redis中
	 * 执行时期：在用户登录后
	 * 
	 * @param sessionId 会话ID
	 */
	AuthUser getAuthUser(String sessionId);
}
