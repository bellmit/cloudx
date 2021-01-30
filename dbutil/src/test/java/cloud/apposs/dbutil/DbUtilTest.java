package cloud.apposs.dbutil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import cloud.apposs.dbutil.sample.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DbUtilTest {
	private static String driverClass = "com.mysql.jdbc.Driver";
	private static String username = "root";
	private static String password = "root";
	private static String url = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&amp;characterEncoding=utf-8";
	
	private static Connection conn;
	
	@BeforeClass
	public static void beforeClass() throws SQLException {
		conn = DbUtil.createConnection(driverClass, url, username, password);
	}
	
	@Test
	public void testQueryToMap() throws SQLException {
		Map<String, Object> datas = DbUtil.query(conn, "SELECT * FROM test", new MapConverter());
		for (Map.Entry<String, Object> data : datas.entrySet()) {
			System.out.println(data.getKey());
			System.out.println(data.getValue());
		}
	}
	
	@Test
	public void testQueryToArray() throws SQLException {
		String[] datas = DbUtil.query(conn, "SELECT * FROM test", new ArrayConverter());
		for (String data : datas) {
			System.out.println(data);
		}
	}
	
	@Test
	public void testQueryToMapList() throws SQLException {
		List<Map<String, Object>> dataList = DbUtil.query(conn, "SELECT * FROM test", new MapListConverter());
		for (Map<String, Object> dataMap : dataList) {
			for (Map.Entry<String, Object> data : dataMap.entrySet()) {
				System.out.println("key : " + data.getKey() + " ==== value : " + data.getValue());
			}
		}
	}
	
	@Test
	public void testQueryToArrayList() throws SQLException {
		List<String[]> dataList = DbUtil.query(conn, "SELECT * FROM test", new ArrayListConverter());
		for (String[] datas : dataList) {
			for (String data : datas) {
				System.out.print(data + "    ");
			}
			System.out.println();
		}
	}
	
	@Test
	public void testQueryToBean() throws SQLException {
		User user = DbUtil.query(conn, "SELECT * FROM test", new BeanConverter<User>(User.class));
		System.out.println(user);
	}
	
	@Test
	public void testQueryToBeanList() throws SQLException {
		List<User> users = DbUtil.query(conn, "SELECT * FROM test WHERE name = ?", new BeanListConverter<User>(User.class), "hong");
		for (User user : users) {
			System.out.println(user);
		}
	}
	
	@Test
	public void testUpdate() throws SQLException {
		int count = DbUtil.update(conn, "UPDATE test SET age = 20 WHERE name = ?", "qun");
		System.out.println(count);
	}
	
	@Test
	public void testBatchUpdate() throws SQLException {
		Object[][] dataList = new Object[3][];
		for (int i = 0; i < 3; i++) {
			Object[] datas = new Object[]{"qun" + i,  25 + i};
			dataList[i] = datas;
		}
		int[] counts = DbUtil.batchUpdate(conn, "INSERT test (name, age) VALUES (?, ?)", dataList);
		for (int count : counts) {
			System.out.println(count);
		}
	}
	
	@Test
	public void testGetAllTables() throws SQLException {
		List<String> tables = DbUtil.getAllTables(conn);
		for (String table : tables) {
			System.out.println(table);
		}
	}
	
	@Test
	public void testGetAllViews() throws SQLException {
		List<String> views = DbUtil.getAllViews(conn);
		for (String view : views) {
			System.out.println(view);
		}
	}
	
	@Test
	public void testTableColumns() throws SQLException {
		List<String> columns = DbUtil.getTableColumns(conn, "test");
		for (String column : columns) {
			System.out.println(column);
		}
	}
	
	@Test
	public void testTableExists() throws SQLException {
		System.out.println(DbUtil.isTableExists(conn, "test"));
	}
	
	@Test
	public void testCreateConnection() throws SQLException {
		Connection conn = DbUtil.createConnection(driverClass, url, username, password);
		System.out.println(conn);
	}
	
	@AfterClass
	public static void afterClass() throws SQLException {
		conn.close();
	}
}
