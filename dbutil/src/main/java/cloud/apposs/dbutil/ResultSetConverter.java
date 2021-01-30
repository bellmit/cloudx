package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 将结果转换成其他对象
 * 
 * @param <T> 要转换成的对象，可为List,Map等
 * @date 2012.06.27
 */
public interface ResultSetConverter<T> {
	/**
	 * 将指定结果集转换成相对应对象
	 * 
	 * @param rs 结果集
	 * @return
	 * @throws SQLException
	 */
	T convert(ResultSet rs) throws SQLException;
}
