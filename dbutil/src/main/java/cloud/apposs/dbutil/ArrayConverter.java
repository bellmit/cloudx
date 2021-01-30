package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;


/**
 * 将结果转换成对象数组，注意！此类只取出数据库中第一条数据集
 */
public class ArrayConverter implements ResultSetConverter<String[]> {
	@Override
	public String[] convert(ResultSet rs) throws SQLException {
		return rs.next() ? toArray(rs) : null;
	}
	
	private String[] toArray(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        String[] result = new String[cols];

        for (int i = 0; i < cols; i++) {
            result[i] = rs.getString(i + 1);
        }

        return result;
    }
}
