package cloud.apposs.cachex.memory.redis.codis;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;

/**
 * {@link RedisConnection}分区，
 * 每个分区代表一个Redis服务的连接池列表，
 * 连接池利用多个Connection分区来高效管理各个Connection
 */
public final class RedisConnectionPartition implements Serializable {
	private static final long serialVersionUID = 8816636552381839623L;
	
	private final RedisPool pool;
	
	/** 所有空闲的Redis连接 */
	private final Queue<RedisConnection> connections = new LinkedBlockingQueue<RedisConnection>();
	
	/** 当前连接池中有多少个Redis连接被创建 */
	private final AtomicInteger createdConnections = new AtomicInteger(0);
	
	/** 
	 * 获取Connection堆栈
	 * 主要用于跟跟踪Connection被系统调用的轨迹
	 * 判断哪些Connection没有及时关闭长期占用资源
	 * 注意此功能需要开启相应的{@link DbConfig#setPoolOperationWatch(boolean)}功能才能跟踪
	 */
	private Map<RedisConnection, String> connectionStackTrace = new ConcurrentHashMap<RedisConnection, String>();
	
	/** 连接池同步锁 */
    private final ReentrantLock mainLock = new ReentrantLock();

	public RedisConnectionPartition(RedisPool pool) {
		this.pool = pool;
	}
	
	public RedisConfig getConfig() {
		return pool.getConfig();
	}
	
	/**
	 * 返回所有创建的Connection连接数
	 * 
	 * @return 所有创建的Connection连接数
	 */
	public int getNumCreatedConnections() {
		return createdConnections.get();
	}
	
	/**
	 * 获取该分区所有可用的Connection连接数
	 * 
	 * @return 所有可用的Connection连接数
	 */
	public int getNumIdleConnections() {
		return connections.size();
	}
	
	/**
	 * 返回所有忙碌Connection连接数（即被应用程序获取并使用Connection）
	 * 
	 * @return 所有忙碌Connection连接数
	 */
	public int getNumBusyConnections() {
		return getNumCreatedConnections() - getNumIdleConnections();
	}
	
	/**
	 * 从分区中销毁所有Connection连接
	 * 
	 * @throws SQLException
	 */
	public void destroyAllConnections() {
		mainLock.lock();
		try{
			// 销毁所有Connection连接
			RedisConnection connection;
			while ((connection = connections.poll()) != null) {
				connection.close();
				// 从空闲Connection集中移除
				connections.remove(connection);
				// Connection创建数递减
				createdConnections.decrementAndGet();
			}
		} finally {
			mainLock.unlock();
		}
	}

	/**
	 * 检索分区中空闲Connection连接，不存在空闲连接时返回null，为系统内部调用
	 * 
	 * @return 分区中空闲Connection连接
	 */
	protected RedisConnection borrowConnection() {
		// 如果开启在取得连接的同时将校验连接的有效性
		// 则在每次获取连接时都进行检查直到Connection集中存在有效连接或者Connection集为空
		if (!pool.getConfig().isTestConnectionOnCheckout()) {
			return connections.poll();
		}
		RedisConnection connection = null;
		while ((connection = connections.poll()) != null) {
			if (isConnectionAlive(connection)) {
				break;
			} else {
				// 触发连接无效监听
				pool.getListeners().fireConnectionInValid(connection);
				// 因为Connection连接在获取时已经出错并被抛弃，此时createdConnections递减
				createdConnections.decrementAndGet();
				// 关闭无效连接
				connection.internalClose();
			}
		}
		return connection;
	}
	
	/**
	 * 在小于maxConnections的情况下添加递增指定acquireIncrement数量的Connection连接
	 * 
	 * @return 成功创建的Connection连接数
	 */
	protected boolean addIfUnderMaxConnectons() {
        mainLock.lock();
        int success = 0;
        try {
        	int acquireIncrement = pool.getConfig().getAcquireIncrement();
            success = doCreateConnections(acquireIncrement);
        } finally {
            mainLock.unlock();
        }
        return success > 0;
	}
	
	/**
	 * 回收Connection连接，为系统内部调用
	 */
	protected void retriveConnection(RedisConnection connection) {
		if (connection == null) {
			return;
		}
		// 如果开启在回收连接的同时将校验连接的有效性，则进行连接的有效性检查，无效直接退出不予回收
		if (pool.getConfig().isTestConnectionOnCheckin()) {
			if (!isConnectionAlive(connection)) {
				pool.getListeners().fireConnectionInValid(connection);
				// 因为Connection连接回收时已经出错并被抛弃，此时createdConnections递减
				createdConnections.decrementAndGet();
				// 关闭无效连接
				connection.internalClose();
				return;
			}
		}
		pool.getListeners().fireConnectionRetrived(connection);
		// 开始回收到池（分区）中
		if (!connections.offer(connection)) {
			connection.internalClose();
		}
	}
	
	/**
	 * 添加Connection操作轨迹
	 * 
	 * @param connection Connection连接
	 * @param trace Connection调用堆栈
	 */
	protected void addConnectionStackTrace(RedisConnection connection, String trace) {
		if (connection != null && trace != null) {
			connectionStackTrace.put(connection, trace);
		}
	}
	
	/**
	 * 移除Connection操作轨迹
	 * 
	 * @param connection Connection连接
	 */
	protected void removeConnectionStackTrace(RedisConnection connection) {
		if (connection != null) {
			connectionStackTrace.remove(connection);
		}
	}
	
	/**
	 * 批量创建Connection连接
	 * 
	 * @param  count 要创建Connection数量
	 * @return 成功创建的Connection数量
	 * @throws SQLException 
	 */
	private int doCreateConnections(int count) {
		int success = 0;
        mainLock.lock();
        try {
        	// 一次性创建N个配置好的连接，并且不能超过最大连接数
        	RedisConfig config = pool.getConfig();
        	int maxConnection = config.getMaxConnections();
			while ((createdConnections.get() < maxConnection && (success < count))) {
				RedisServer server = pool.selectRandomServer();
				String host = server.getHost();
				int port = server.getPort();
				int connectTimeout = config.getConnectTimeout();
				int recvTimeout = config.getRecvTimeout();
				final RedisConnection connection = 
					new RedisConnection(this, host, port, connectTimeout, recvTimeout);
				if (connections.add(connection)) {
					pool.getListeners().fireConnectionCreated(connection);
					success++;
					// Connection创建数递增
					createdConnections.incrementAndGet();
				}
			}
        } finally {
        	mainLock.unlock();
        }
		return success;
	}
	
	/**
	 * 判断Connection连接是否为有效连接
	 * 实际上是发送一条查询语句进行查询，如果抛出异常则表示此Connection为无效
	 * 
	 * @param  connection
	 * @return 有效连接返回true
	 */
	private boolean isConnectionAlive(RedisConnection connection) {
		return connection.isConnected();
	}
}
