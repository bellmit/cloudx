package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * 将结果转换成<code>Map</code>，注意！此类只取出数据库中第一条数据集
 */
public class MapConverter implements ResultSetConverter<Map<String, Object>> {
	@Override
	public Map<String, Object> convert(ResultSet rs) throws SQLException {
		return rs.next() ? this.toMap(rs) : null;
	}
	
	public Map<String, Object> toMap(ResultSet rs) throws SQLException {
        Map<String, Object> result = new HashMap<String, Object>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();

        for (int i = 1; i <= cols; i++) {
            result.put(rsmd.getColumnName(i), rs.getObject(i));
        }

        return result;
    }
}
