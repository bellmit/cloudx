package cloud.apposs.cachex.storage.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.util.ResourceUtil;

/**
 * 数据库连接池，支持JNDI和Tomcat连接池配置
 */
public class DbPoolDataSource implements DataSource, ObjectFactory {
	private static final long serialVersionUID = -8488746754798435398L;
	
	/** 读写锁 */
	private ReadWriteLock mainLock = new ReentrantReadWriteLock();
	
	private transient volatile DbPool pool = null;
	
	private Map<UsernamePassword, DbPool> multiDbPool = 
		new ConcurrentHashMap<UsernamePassword, DbPool>();
	
	private PrintWriter logWriter = null;

	private final Logger logger = Logger.getLogger(DbPoolDataSource.class.getName());
	
	public DbPoolDataSource() {
	}
	
	public DbPoolDataSource(DbConfig config) {
		// 初始化数据库连接池
		try {
			initPoolIfNull(config);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password)
			throws SQLException {
		UsernamePassword key = new UsernamePassword(username, password);
		DbPool pool = multiDbPool.get(key);
		if (pool == null) {
			DbConfig config = new DbConfig();
			config.setUsername(username);
			config.setPassword(password);
			
			pool = new DbPool(config);
			
			multiDbPool.put(key, pool);
		}
		return pool.getConnection();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return this.logWriter;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException("getLoginTimeout is unsupported.");
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return logger;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.logWriter = out;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout is unsupported.");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	@Override
	public Object getObjectInstance(Object object, Name name, Context nameCtx,
			Hashtable<?, ?> environment) throws Exception {
		Reference ref = (Reference) object;
		Enumeration<RefAddr> addrs = ref.getAll();
		Properties props = new Properties();
		while (addrs.hasMoreElements()) {
			RefAddr addr = addrs.nextElement();
			props.put(addr.getType(), addr.getContent());
		}
		DbConfig config = new DbConfig();
		ResourceUtil.loadProperties(config, props);

		return new DbPoolDataSource(config);
	}
	
	/**
	 * 在{@link #pool}为null的情况下初始化
	 * 
	 * @throws SQLException
	 */
	private void initPoolIfNull(DbConfig config) throws SQLException {
		mainLock.readLock().lock();
		if (pool == null) {
			mainLock.readLock().unlock();
			mainLock.writeLock().lock();
			if (pool == null) {
				pool = new DbPool(config);
			}
			// 写锁解除
			mainLock.writeLock().unlock();
		} else {
			// 读锁解除
			mainLock.readLock().unlock();
		}
	}
	
	public DbPool getPool() {
		return pool;
	}
	
	public void close(){
		if (pool != null){
			pool.shutdown();
		}
		for (DbPool pool : multiDbPool.values()) {
			pool.shutdown();
		}
		multiDbPool.clear();
	}
	
	class UsernamePassword {
		private String username;
		
		private String password;

		public UsernamePassword(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof UsernamePassword)) {
				return false;
			}
			
			UsernamePassword that = (UsernamePassword) obj;
			return (username != null && username.equals(that.getUsername()))
				&& (password != null && password.equals(that.getPassword()));
		}
		
		@Override
		public int hashCode() {
			return (username + password).hashCode();
		}
	}
}
