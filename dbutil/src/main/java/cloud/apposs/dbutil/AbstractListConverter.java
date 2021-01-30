package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * 将结果转换成对象数组集，具体每条结果集的转换形式由子类实现
 */
public abstract class AbstractListConverter<T> implements ResultSetConverter<List<T>> {
	@Override
	public List<T> convert(ResultSet rs) throws SQLException {
		List<T> rows = new ArrayList<T>();
        while (rs.next()) {
            rows.add(convertRow(rs));
        }
        return rows;
	}
	
	protected abstract T convertRow(ResultSet rs) throws SQLException;
}
