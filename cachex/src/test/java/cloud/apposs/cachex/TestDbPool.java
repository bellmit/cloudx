package cloud.apposs.cachex;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.cachex.storage.jdbc.DbPool;

/**
 * https://blog.csdn.net/xzknet/article/details/49127701
 */
public class TestDbPool {
	public static final String JDBC_URL = 
		"jdbc:mysql://172.17.1.236:9091/market?useUnicode=true&characterEncoding=UTF8&connectTimeout=1000&socketTimeout=3000";
	
	@Test
	public void testMysql() throws Exception {
		DbConfig config = new DbConfig();
		config.setDriverClass("com.mysql.jdbc.Driver");
		config.setJdbcUrl(JDBC_URL);
		config.setUsername("root");
		config.setPassword("DB@0nline");
		config.setTestConnectionOnCheckout(false);
		config.setPoolOperationWatch(false);
		config.setAcquireIncrement(1);
		config.setStatementsCacheSize(12);
		DbPool pool = new DbPool(config);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			Connection conn = pool.getConnection();
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery("select * from product limit 1,5");
			while (rs.next()) {
				rs.getString(2);
			}
			rs.close();
			statement.close();
			conn.close();
		}
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
		pool.shutdown();
	}
	
	@Test
	public void testPreparedStatement() throws Exception {
		DbConfig config = new DbConfig();
		config.setDriverClass("com.mysql.jdbc.Driver");
		config.setJdbcUrl(JDBC_URL);
		config.setUsername("root");
		config.setPassword("root");
		config.setTestConnectionOnCheckout(false);
		config.setPoolOperationWatch(false);
		config.setAcquireIncrement(1);
		config.setStatementsCacheSize(12);
		DbPool pool = new DbPool(config);

		Connection conn = pool.getConnection();
		String sql = "select * from user where name like ?";
		PreparedStatement statement = conn.prepareStatement(sql);
		statement.setString(1, "%way%");
		ResultSet rs = statement.executeQuery();
		while (rs.next()) {
			String username = rs.getString(2);
			System.out.println(username);
		}
		rs.close();
		statement.close();
		conn.close();
		pool.shutdown();
	}
	
	@Test
	public void testRawSql() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
	        Connection conn = DriverManager.getConnection(JDBC_URL, "root", "root");
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery("select * from user limit 1,5");
			while (rs.next()) {
				String username = rs.getString(2);
				System.out.println(username);
			}
			rs.close();
			statement.close();
			conn.close();
		}
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testSqlite() throws Exception {
		DbConfig config = new DbConfig();
		config.setDriverClass("org.sqlite.JDBC");
		config.setJdbcUrl("jdbc:sqlite://C:/world0.tdb");
		config.setTestConnectionOnCheckout(false);
		config.setPoolOperationWatch(false);
		config.setAcquireIncrement(1);
		config.setStatementsCacheSize(12);
		DbPool pool = new DbPool(config);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			Connection conn = pool.getConnection();
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery("select * from user;"); // 查询数据
			while (rs.next()) { // 将查询到的数据打印出来
				rs.getString("name");
//				System.out.print("name = " + rs.getString("name") + ", "); // 列属性一
//				System.out.println("salary = " + rs.getString("salary")); // 列属性二
			}
			rs.close();
			conn.close();
		}
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testRawSqlite() throws Exception {
		String jdbcUrl = "jdbc:sqlite://C:/world0.tdb";
		Class.forName("org.sqlite.JDBC");
		long start = System.currentTimeMillis();
		Connection conn = DriverManager.getConnection(jdbcUrl);
		Statement statement = conn.createStatement();
		statement.executeUpdate("drop table if exists user;");
		statement.executeUpdate("create table if not exists user(id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(50),class int(11));");
		statement.close();
		conn.close();
//		for (int i = 0; i < 100; i++) {
//			// 1 建立一个数据库名zieckey.db的连接，如果不存在就在当前目录下创建之
//			Connection conn = DriverManager.getConnection(jdbcUrl);
//			Statement statement = conn.createStatement();
////			statement.executeUpdate("drop table if exists user;");
////			statement.executeUpdate("create table if not exists user(name varchar(20), salary int);");
//			statement.executeUpdate("insert into user values('ZhangSan',8000);"); // 插入数据
//			statement.executeUpdate("insert into user values('LiSi',7800);");
//			statement.executeUpdate("insert into user values('WangWu',5800);");
//			statement.executeUpdate("insert into user values('ZhaoLiu',9100);");
////			ResultSet rs = statement.executeQuery("select * from user;"); // 查询数据
////			System.out.println("创建表结构录入数据操作演示：");
////			while (rs.next()) { // 将查询到的数据打印出来
////				rs.getString("name");
////				System.out.print("name = " + rs.getString("name") + ", "); // 列属性一
////				System.out.println("salary = " + rs.getString("salary")); // 列属性二
////			}
////			rs.close();
////			conn.close();
//		}
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testMysqlTread() throws Exception {
		DbConfig config = new DbConfig();
		config.setDriverClass("com.mysql.jdbc.Driver");
		config.setJdbcUrl(JDBC_URL);
		config.setUsername("root");
		config.setPassword("root");
		config.setTestConnectionOnCheckout(false);
		config.setPoolOperationWatch(false);
//		config.setAcquireIncrement(1);
		config.setStatementsCacheSize(12);
		DbPool pool = new DbPool(config);
		long start = System.currentTimeMillis();
		
		int count = 100;
		CountDownLatch cdl = new CountDownLatch(count);
		for (int i = 0; i < count; i++) {
			new DbThread("Thread" + i, pool, cdl).start();
		}
		cdl.await();
		
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
		pool.shutdown();
	}
	
	static class DbThread extends Thread {
		private final String name;
		
		private final DbPool pool;
		
		private final CountDownLatch cdl;
		
		public DbThread(String name, DbPool pool, CountDownLatch cdl) {
			this.name = name;
			this.pool = pool;
			this.cdl = cdl;
		}
		
		@Override
		public void run() {
			try {
				for (int i = 0; i < 1000; i++) {
					Connection conn = pool.getConnection();
					Statement statement = conn.createStatement();
					ResultSet rs = statement.executeQuery("select * from user limit 1,5");
					while (rs.next()) {
						String username = rs.getString(2);
						System.out.println(name + ":" + username);
					}
					rs.close();
					statement.close();
					conn.close();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			cdl.countDown();
		}
	}
}
