package cloud.apposs.cachex.storage.jdbc;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

/**
 * JDBC数据库连接池
 */
public class DbPool implements Serializable {
	private static final long serialVersionUID = -5858413310905903601L;
	
	/** 数据库配置 */
	private final DbConfig config;
	
	/** 所有空闲的Connection集 */
	private final Queue<ConnectionWrapper> connections = new LinkedBlockingQueue<ConnectionWrapper>();

	/** 连接池同步锁 */
    private final ReentrantLock mainLock = new ReentrantLock();
    
    /** 数据库连接池是否正在运行 */
	private volatile boolean running = true;
	
	/** 当前连接池中有多少个Connection连接被创建 */
	private final AtomicInteger createdConnections = new AtomicInteger(0);
	
	/**
	 * 各Connection连接监听，包括连接满，连接异常等
	 */
	private final ConnectionListenerSupport listeners = new ConnectionListenerSupport();
	
	/** 
	 * 获取Connection堆栈
	 * 主要用于跟跟踪Connection被系统调用的轨迹
	 * 判断哪些Connection没有及时关闭长期占用资源
	 * 注意此功能需要开启相应的{@link DbConfig#setPoolOperationWatch(boolean)}功能才能跟踪
	 */
	private Map<ConnectionWrapper, String> connectionStackTrace = new ConcurrentHashMap<ConnectionWrapper, String>();

	public DbPool(DbConfig config) throws SQLException {
		SysUtil.checkNotNull(config, "config");
		this.config = config;
		// 注册驱动
		doRegisterDriver();
	}
	
	public DbConfig getConfig() {
		return config;
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * 获取数据库连接池空闲Connection
	 */
	public Connection getConnection() throws SQLException {
		// 判断数据库连接池是否已经关闭
		if (!running){
			throw new SQLException("Database pool has been shutdown");
		}
		
		ConnectionWrapper connection = null;
		
		// 从连接池队列中poll出一个空闲的connection对象
		connection = borrowConnection();
		
		// 已经没有空闲连接,在没有达到Connection连接上限的情况下
		// 向队列中插入一个指定DbPoolConfig#acquireIncrement数量的connection
		if (connection == null) {
			addIfUnderMaxConnectons();
			connection = borrowConnection();
		}
		
		// 已经没有可用连接，直接返回空
		// 不阻塞等待，避免影响服务性能反而导致请求堆积造成雪崩
		if (connection == null) {
			return null;
		}
		
		// 激活Connection连接
		connection.active();
		
		// 记录操作跟踪日志
		if (config.isPoolOperationWatch()) {
			StringBuilder stringBuilder = new StringBuilder();
			// 记录Connection操作轨迹
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			for(int i = 0; i < trace.length; i++){
				stringBuilder.append(" ").append(trace[i]).append("\r\n");
			}

			addConnectionStackTrace(connection, stringBuilder.toString());
		}
		
		return connection;
	}
	
	/**
	 * 获取原始的未经包装的JDBC Connection，
	 * 注意该连接生命周期不归入连接池管理，仅为业务提供生成连接
	 */
	public Connection getRawConnection() throws SQLException {
		String url = config.getJdbcUrl();
		String username = config.getUsername();
		String password = config.getPassword();
		Connection connection = DriverManager.getConnection(url, username, password);
		
		if (config.getAutoCommit() != null) {
			connection.setAutoCommit(config.getAutoCommit());
		}
		if (config.getReadOnly() != null) {
			connection.setReadOnly(config.getReadOnly());
		}
		if (config.getTransactionIsolation() != null) {
			connection.setTransactionIsolation(config.getTransactionIsolation());
		}
		
		return connection;
	}
	
	public ConnectionListenerSupport getListenerSupport() {
		return listeners;
	}

	/**
	 * 获取Connection操作轨迹
	 */
	public Map<ConnectionWrapper, String> getConnectionStackTrace() {
		return this.connectionStackTrace;
	}
	
	/**
	 * 添加Connection操作轨迹
	 * 
	 * @param connection Connection连接
	 * @param trace Connection调用堆栈
	 */
	public void addConnectionStackTrace(ConnectionWrapper connection, String trace) {
		if (connection != null && trace != null) {
			connectionStackTrace.put(connection, trace);
		}
	}
	
	/**
	 * 移除Connection操作轨迹
	 * 
	 * @param connection Connection连接
	 */
	public void removeConnectionStackTrace(ConnectionWrapper connection) {
		if (connection != null) {
			connectionStackTrace.remove(connection);
		}
	}
	
	/**
	 * Connnection回收，主要用于程序调用{@link #getConnection()}后忘记释放Connection时占用资源
	 * 注意，此功能需要开启相应的{@link DbConfig#setPoolOperationWatch(boolean)}
	 * 
	 * @throws SQLException 
	 */
	public void connectionCollect() throws SQLException {
		for (ConnectionWrapper connection : connectionStackTrace.keySet()) {
			connection.close();
		}
		connectionStackTrace.clear();
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
	 * 关闭所有Connection连接
	 */
	public void terminateAllConnections(){
		mainLock.lock();
		try{
			// 销毁所有Connection连接
			ConnectionWrapper connection;
			while ((connection = connections.poll()) != null) {
				try {
					connection.internalClose();
				} catch (SQLException e) {
				}
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
	 * 关闭数据库连接池
	 */
	public synchronized void shutdown(){
		if (!running) {
			return;
		}
		running = false;
		terminateAllConnections();
	}
	
	@Override
	public String toString() {
		StringBuffer info = new StringBuffer();
		info.append("{");
		info.append("driverClass:").append(config.getDriverClass()).append(", ");
		info.append("jdbcUrl:").append(config.getJdbcUrl()).append(", ");
		info.append("username:").append(config.getUsername().charAt(0)).append("****").append(", ");
		info.append("password:").append(config.getPassword().charAt(0)).append("****").append(", ");
		info.append("numCreated:").append(getNumCreatedConnections()).append(", ");
		info.append("numIdle:").append(getNumIdleConnections()).append(", ");
		info.append("numBusy:").append(getNumBusyConnections());
		info.append("}");
		return info.toString();
	}
	
	/**
	 * 注册JDBC驱动
	 */
	private void doRegisterDriver() throws SQLException {
		String driverClass = config.getDriverClass();
		if (StrUtil.isEmpty(driverClass)) {
			throw new SQLException("JDBC driver is null");
		}
		try {
			try {
				Class.forName(driverClass);
			} catch (ClassNotFoundException cnfe) {
				Thread.currentThread().getContextClassLoader().loadClass(driverClass);
			}
		} catch (Throwable t) {
			String message = "Cannot Load JDBC driver class '" + driverClass + "'";
			throw new SQLException(message, t);
		}
	}
	
	/**
	 * 批量创建Connection连接
	 * 
	 * @param  count 要创建Connection数量
	 * @return 成功创建的Connection数量
	 * @throws SQLException 
	 */
	private int doCreateConnections(int count) throws SQLException {
		int success = 0;
        mainLock.lock();
        try {
        	// 一次性创建N个配置好的连接，并且不能超过最大连接数
			while ((createdConnections.get() < config.getMaxConnections() && (success < count))) {
				final ConnectionWrapper connection = 
					new ConnectionWrapper(this, config.getJdbcUrl(), 
					config.getUsername(), config.getPassword());
				if (connections.add(connection)) {
					listeners.fireConnectionCreated(connection);
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
	 * 检索分区中空闲Connection连接，不存在空闲连接时返回null，为系统内部调用
	 * 
	 * @return 分区中空闲Connection连接
	 */
	private ConnectionWrapper borrowConnection() throws SQLException {
		// 如果开启在取得连接的同时将校验连接的有效性
		// 则在每次获取连接时都进行检查直到Connection集中存在有效连接或者Connection集为空
		if (!config.isTestConnectionOnCheckout()) {
			return connections.poll();
		}
		ConnectionWrapper connection = null;
		while ((connection = connections.poll()) != null) {
			if (isConnectionAlive(connection.getRawConnection())) {
				break;
			} else {
				// 触发连接无效监听
				listeners.fireConnectionInValid(connection);
				// 因为Connection连接在获取时已经出错并被抛弃，此时createdConnections递减
				createdConnections.decrementAndGet();
				// 关闭无效连接
				connection.internalClose();
			}
		}
		return connection;
	}
	
	/**
	 * 回收Connection连接，为系统内部调用
	 * 
	 * @param connection Connection连接
	 * @throws SQLException 
	 */
	protected void retriveConnection(ConnectionWrapper connection) throws SQLException {
		if (connection == null) {
			return;
		}
		// 如果开启在回收连接的同时将校验连接的有效性，则进行连接的有效性检查，无效直接退出不予回收
		if (config.isTestConnectionOnCheckin()) {
			if (!isConnectionAlive(connection.getRawConnection())) {
				listeners.fireConnectionInValid(connection);
				// 因为Connection连接回收时已经出错并被抛弃，此时createdConnections递减
				createdConnections.decrementAndGet();
				// 关闭无效连接
				connection.internalClose();
				return;
			}
		}
		listeners.fireConnectionRetrived(connection);
		// 开始回收到池（分区）中
		if (!connections.offer(connection)) {
			connection.internalClose();
		}
	}
	
	/**
	 * 判断Connection连接是否为有效连接
	 * 实际上是发送一条查询语句进行查询，如果抛出异常则表示此Connection为无效
	 * 
	 * @param  connection
	 * @return 有效连接返回true
	 */
	public boolean isConnectionAlive(Connection connection) {
		if (connection == null) {
			return false;
		}
		
		ResultSet rs = null;
		Statement stmt = null;
		boolean result = false;
		try {
			rs = connection.getMetaData().getTables(null, null, "ALIVETEST", new String[]{"TABLE"});
			result = true;
		} catch (SQLException e) {
			result = false;
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (rs != null) { 
					rs.close();
				}
			} catch (SQLException e) {
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * 在小于maxConnections的情况下添加递增指定acquireIncrement数量的Connection连接
	 * 
	 * @return 成功创建的Connection连接数
	 * @throws SQLException
	 */
	private boolean addIfUnderMaxConnectons() throws SQLException {
        mainLock.lock();
        int success = 0;
        try {
            success = doCreateConnections(config.getAcquireIncrement());
        } finally {
            mainLock.unlock();
        }
        return success > 0;
	}
}
