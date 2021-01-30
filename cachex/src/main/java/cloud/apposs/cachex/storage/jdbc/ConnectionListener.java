package cloud.apposs.cachex.storage.jdbc;

import java.sql.SQLException;
import java.util.EventListener;

/**
 * 连接监听
 */
public interface ConnectionListener extends EventListener {
	/**
	 * 数据库连接已经失效，即该连接已经断开
	 */
	void connectionInvalid(ConnectionWrapper connection);

	/**
	 * 连接池发生致命错误，关闭所有连接，一般是数据库宕机或者被重启
	 */
	void poolInvalid(SQLException e);

	/**
	 * 创建数据库连接时的监听
	 */
	void connectionCreated(ConnectionWrapper connection);
	
	/**
	 * 回收数据库连接时的监听
	 */
	void connectionRetrived(ConnectionWrapper connection);
}
