package cloud.apposs.logger.appender;

import cloud.apposs.logger.Appender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 数据库日志会话存储
 */
public class DatabaseAppender extends Appender {
	private String driver;

	private String url;
	
	private String user;

	private String password;
	
	private String sql;
	
	private Connection connection;
	
	private Statement stmt;
	
	public DatabaseAppender(String driver, String url, String user, String password, String sql) {
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;
		this.sql = sql;
		
		this.setDriver(driver);
		this.connection = this.getConnection();
	}
	
	@Override
	public void append(List<String> msgList) {
		if (msgList != null) {
			try {
				Connection conn = getConnection();
				Statement stmt = getStatement();
				conn.setAutoCommit(false);
				for (String msg : msgList) {
					stmt.addBatch(msg);
				}
				stmt.executeBatch();
				conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void close() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
			if (stmt != null && !stmt.isClosed()) {
				stmt.close();
			}
		} catch (SQLException e) {
		        e.printStackTrace();
		}
	}
	
	public String getDriver() {
		return driver;
	}
	
	public void setDriver(String driverClass) {
		try {
			Class.forName(driverClass);
		} catch (Exception e) {
	      e.printStackTrace();
		}
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSql() {
		return sql;
	}

	private Connection getConnection() {
	      if (!DriverManager.getDrivers().hasMoreElements())
	    	  setDriver("sun.jdbc.odbc.JdbcOdbcDriver");
	      if (connection == null) {
	    	  try {
				connection = DriverManager.getConnection(url, user, password);
	    	  } catch (Exception e) {
	    		  e.printStackTrace();
	    	  }
	      }

	      return connection;
	  }
	
	private Statement getStatement() {
		if (stmt == null) {
			Connection conn = getConnection();
			try {
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
						   ResultSet.CONCUR_UPDATABLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return stmt;
	  }
}
