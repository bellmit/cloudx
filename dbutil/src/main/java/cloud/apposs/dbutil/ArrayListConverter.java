package cloud.apposs.dbutil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ArrayListConverter extends AbstractListConverter<String[]> {
	@Override
	protected String[] convertRow(ResultSet rs) throws SQLException {
		ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        String[] result = new String[cols];

        for (int i = 0; i < cols; i++) {
            result[i] = rs.getString(i + 1);
        }

        return result;
	}
}
