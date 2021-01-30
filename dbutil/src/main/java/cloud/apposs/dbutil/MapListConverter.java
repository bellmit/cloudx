package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 将结果转换成<code>Map</code>数组集
 */
public class MapListConverter extends AbstractListConverter<Map<String, Object>> {
	@Override
	protected Map<String, Object> convertRow(ResultSet rs) throws SQLException {
		Map<String, Object> result = new HashMap<String, Object>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();

        for (int i = 1; i <= cols; i++) {
            result.put(rsmd.getColumnName(i), rs.getObject(i));
        }

        return result;
	}
}
