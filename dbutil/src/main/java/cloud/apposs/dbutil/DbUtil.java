package cloud.apposs.dbutil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作辅助类，封装对数据库的查找、关闭、更新的简单接口操作
 * 
 * @author qun1988@gmail.com
 * @date 2012.06.27
 */
public class DbUtil {
	private DbUtil() {
	}
	
	public static <T> T query(Connection conn, String sql,
			ResultSetConverter<T> rsc) throws SQLException {
		return query(conn, false, sql, rsc, (Object[]) null);
	}

	public static <T> T query(Connection conn, String sql,
			ResultSetConverter<T> rsc, Object... params) throws SQLException {
		return query(conn, false, sql, rsc, params);
	}

	/**
	 * 查询结果集
	 * 
	 * @param conn 数据库连接
	 * @param closeConn 是否关闭数据库连接
	 * @param sql SQL语句
	 * @param rsc 结果集转换器
	 * @param params 查询参数
	 */
	public static <T> T query(Connection conn, boolean closeConn, String sql,
			ResultSetConverter<T> rsc, Object... params) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}

		if (sql == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null SQL statement");
		}

		if (rsc == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null ResultSetHandler");
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		T result = null;

		try {
			stmt = conn.prepareStatement(sql);
			fillStatement(stmt, params);
			rs = stmt.executeQuery();
			result = rsc.convert(rs);
		} finally {
			try {
				close(rs);
			} finally {
				close(stmt);
				if (closeConn) {
					close(conn);
				}
			}
		}

		return result;
	}

	public static String[] getArray(Connection conn, String sql)
			throws SQLException {
		return query(conn, false, sql, new ArrayConverter(), (Object[]) null);
	}

	public static List<String[]> getArrayList(Connection conn, String sql)
			throws SQLException {
		return query(conn, false, sql, new ArrayListConverter(), (Object[]) null);
	}
	
	public static Map<String, Object> getMap(Connection conn, String sql)
		throws SQLException {
		return query(conn, false, sql, new MapConverter(), (Object[]) null);
	}
	
	public static List<Map<String, Object>> getMapList(Connection conn, String sql)
		throws SQLException {
		return query(conn, false, sql, new MapListConverter(), (Object[]) null);
	}
	
	public static <T> T getBean(Connection conn, String sql, Class<T> type) throws SQLException {
		return query(conn, false, sql, new BeanConverter<T>(type), (Object[]) null);
	}
	
	public static <T> List<T> getBeanList(Connection conn, String sql, Class<T> type) throws SQLException {
		return query(conn, false, sql, new BeanListConverter<T>(type), (Object[]) null);
	}

	public static int update(Connection conn, String sql) throws SQLException {
		return update(conn, false, sql, (Object[]) null);
	}

	public static int update(Connection conn, String sql, Object... params)
			throws SQLException {
		return update(conn, false, sql, params);
	}

	/**
	 * 更新SQL语句
	 */
	public static int update(Connection conn, boolean closeConn, String sql,
			Object... params) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}

		if (sql == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null SQL statement");
		}

		PreparedStatement stmt = null;
		int rows = 0;

		try {
			stmt = conn.prepareStatement(sql);
			fillStatement(stmt, params);
			rows = stmt.executeUpdate();
		} finally {
			close(stmt);
			if (closeConn) {
				close(conn);
			}
		}

		return rows;
	}

	public static int[] batchUpdate(Connection conn, String sql, Object[][] params)
			throws SQLException {
		return batchUpdate(conn, false, sql, params);
	}

	/**
	 * 批量更新SQL语句
	 */
	public static int[] batchUpdate(Connection conn, boolean closeConn, String sql,
			Object[][] params) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}

		if (sql == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null SQL statement");
		}

		if (params == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException(
					"Null parameters. If parameters aren't need, pass an empty array.");
		}

		PreparedStatement stmt = null;
		int[] rows = null;
		try {
			stmt = conn.prepareStatement(sql);

			for (int i = 0; i < params.length; i++) {
				fillStatement(stmt, params[i]);
				stmt.addBatch();
			}
			rows = stmt.executeBatch();

		} finally {
			close(stmt);
			if (closeConn) {
				close(conn);
			}
		}

		return rows;
	}

	private static void fillStatement(PreparedStatement stmt, Object... params)
			throws SQLException {
		ParameterMetaData pmd = stmt.getParameterMetaData();
		int stmtCount = pmd.getParameterCount();
		int paramsCount = params == null ? 0 : params.length;

		if (stmtCount != paramsCount) {
			throw new SQLException("Wrong number of parameters: expected "
					+ stmtCount + ", was given " + paramsCount);
		}

		if (params == null) {
			return;
		}

		for (int i = 0; i < params.length; i++) {
			if (params[i] != null) {
				stmt.setObject(i + 1, params[i]);
			} else {
				int sqlType = Types.VARCHAR;
				sqlType = pmd.getParameterType(i + 1);
				stmt.setNull(i + 1, sqlType);
			}
		}
	}
	
	/**
	 * 判断指定表名是否存在
	 */
	public static boolean isTableExists(Connection conn, String tablename) throws SQLException {
		return isTableExists(conn, false, tablename);
	}
	
	/**
	 * 判断指定表名是否存在
	 */
	public static boolean isTableExists(Connection conn,
			boolean closeConn, String tablename) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}
		
		if (tablename == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null SQL statement");
		}
		
		ResultSet rs = null;
		try {
			DatabaseMetaData dbMetaData = conn.getMetaData();
			String[] types = {"TABLE"};
			rs = dbMetaData.getTables(null, null, tablename, types);
			return rs.next();
		} finally {
			try {
				close(rs);
			} finally {
				if (closeConn) {
					close(conn);
				}
			}
		}
	}
	
	/**
	 * 获取指定数据库中所有的表名
	 * 
	 * @param conn 数据库连接句柄
	 */
	public static List<String> getAllTables(Connection conn) throws SQLException {
		return getAllTables(conn, false);
	}
	
	/**
	 * 获取指定数据库中所有的表名
	 * 
	 * @param conn 数据库连接句柄
	 * @param closeConn 是否关闭数据库连接
	 * @return
	 * @throws SQLException
	 */
	public static List<String> getAllTables(Connection conn, 
			boolean closeConn) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}

		ResultSet rs = null;
		List<String> tableList = new ArrayList<String>();

		try {
			DatabaseMetaData dbMetaData = conn.getMetaData();
			String[] types = {"TABLE"};
			rs = dbMetaData.getTables(null, null, "%", types);
	        while (rs.next()) {
	        	tableList.add(rs.getString("TABLE_NAME"));
	        }
		} finally {
			try {
				close(rs);
			} finally {
				if (closeConn) {
					close(conn);
				}
			}
		}

		return tableList;
	}
	
	/**
	 * 获取指定数据库中所有视图
	 * 
	 * @param conn 数据库连接句柄
	 */
	public static List<String> getAllViews(Connection conn) throws SQLException {
		return getAllViews(conn, false);
	}
	
	/**
	 * 获取指定数据库中所有视图
	 * 
	 * @param conn 数据库连接句柄
	 * @param closeConn 是否关闭数据库连接
	 */
	public static List<String> getAllViews(Connection conn, 
			boolean closeConn) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}

		ResultSet rs = null;
		List<String> tableList = new ArrayList<String>();

		try {
			DatabaseMetaData dbMetaData = conn.getMetaData();
			String[] types = {"VIEW"};
			rs = dbMetaData.getTables(null, null, "%", types);
	        while (rs.next()) {
	        	tableList.add(rs.getString("TABLE_NAME"));
	        }
		} finally {
			try {
				close(rs);
			} finally {
				if (closeConn) {
					close(conn);
				}
			}
		}

		return tableList;
	}
	
	/**
	 * 获取指定数据表列集
	 * 
	 * @param conn 数据库连接句柄
	 */
	public static List<String> getTableColumns(Connection conn, String tablename) throws SQLException {
		return getTableColumns(conn, false, tablename);
	}
	
	/**
	 * 获取指定数据库中所有视图
	 * 
	 * @param conn 数据库连接句柄
	 * @param closeConn 是否关闭数据库连接
	 */
	public static List<String> getTableColumns(Connection conn, 
			boolean closeConn, String tablename) throws SQLException {
		if (conn == null) {
			throw new SQLException("Null connection");
		}
		
		if (tablename == null) {
			if (closeConn) {
				close(conn);
			}
			throw new SQLException("Null SQL statement");
		}

		ResultSet rs = null;
		List<String> tableList = new ArrayList<String>();

		try {
			DatabaseMetaData dbMetaData = conn.getMetaData();
			rs = dbMetaData.getColumns(null, null, tablename, "%");
	        while (rs.next()) {
	        	tableList.add(rs.getString("COLUMN_NAME"));
	        }
		} finally {
			try {
				close(rs);
			} finally {
				if (closeConn) {
					close(conn);
				}
			}
		}

		return tableList;
	}
	
	/**
	 * 创建数据库连接
	 * 
	 * @param driverClass 
	 * @param url
	 * @param username 用户名
	 * @param password 密码
	 * @return 数据库连接
	 * @throws SQLException
	 */
	public static Connection createConnection(String driverClass, String url, 
			String username, String password) throws SQLException {
		try {
			Class.forName(driverClass);
			Connection connection = DriverManager.getConnection(url, username, password);
			return connection;
		} catch (ClassNotFoundException e) {
			throw new SQLException(e);
		}
	}

	/**
	 * 关闭数据库连接
	 */
	public static void close(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
		}
	}

	public static void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
		}
	}
	
	public static void close(Statement stmt) {
		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException e) {
		}
	}

	public static void close(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
		}
	}
}
