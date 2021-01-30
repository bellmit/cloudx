package cloud.apposs.cachex.memory.redis.codis;

import java.util.EventListener;

public interface RedisConnectionListener extends EventListener {
	/**
	 * 数据库连接已经失效，即该连接已经断开
	 */
	void connectionInvalid(RedisConnection connection);

	/**
	 * 创建数据库连接时的监听
	 */
	void connectionCreated(RedisConnection connection);
	
	/**
	 * 回收数据库连接时的监听
	 */
	void connectionRetrived(RedisConnection connection);
}
