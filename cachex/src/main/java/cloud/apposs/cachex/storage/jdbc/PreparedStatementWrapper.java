package cloud.apposs.cachex.storage.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class PreparedStatementWrapper extends StatementWrapper implements
		PreparedStatement {
	private PreparedStatement preparedStatement;
	
	public PreparedStatementWrapper(ConnectionWrapper connection, PreparedStatement preparedStatement) {
		super(connection, preparedStatement);
		this.preparedStatement = preparedStatement;
	}
	
	public PreparedStatementWrapper(ConnectionWrapper connection, 
			PreparedStatement preparedStatement, String sql, String cacheKey) {
		super(connection, preparedStatement, sql, cacheKey);
		this.preparedStatement = preparedStatement;
	}

	@Override
	public void addBatch() throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.addBatch();
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void clearParameters() throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.clearParameters();
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public boolean execute() throws SQLException {
		checkClosed();
		try {
			boolean result = this.preparedStatement.execute();
			return result;
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		checkClosed();
		try {
			ResultSet result = this.preparedStatement.executeQuery();
			return result;
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public int executeUpdate() throws SQLException {
		checkClosed();
		try {
			int result = this.preparedStatement.executeUpdate();
			return result;
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		checkClosed();
		try {
			return this.preparedStatement.getMetaData();
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		checkClosed();
		try {
			return this.preparedStatement.getParameterMetaData();
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setArray(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setAsciiStream(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setAsciiStream(parameterIndex, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setAsciiStream(parameterIndex, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBigDecimal(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBinaryStream(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBinaryStream(parameterIndex, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBinaryStream(parameterIndex, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBlob(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBlob(parameterIndex, inputStream);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBlob(parameterIndex, inputStream, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBoolean(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setByte(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setBytes(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setCharacterStream(parameterIndex, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setCharacterStream(parameterIndex, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader,
			long length) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setCharacterStream(parameterIndex, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setClob(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setClob(parameterIndex, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setClob(parameterIndex, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setDate(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setDate(parameterIndex, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setDouble(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setFloat(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setInt(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setLong(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNCharacterStream(parameterIndex, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value,
			long length) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNCharacterStream(parameterIndex, value, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNClob(parameterIndex, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNClob(parameterIndex, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNClob(parameterIndex, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNString(int parameterIndex, String value)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNString(parameterIndex, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNull(parameterIndex, sqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setNull(parameterIndex, sqlType, typeName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setObject(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setObject(parameterIndex, x, targetSqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scaleOrLength) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setRef(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setRowId(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setSQLXML(parameterIndex, xmlObject);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setShort(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setString(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setTime(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setTime(parameterIndex, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setTimestamp(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setTimestamp(parameterIndex, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setURL(parameterIndex, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	@Deprecated
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		checkClosed();
		try {
			this.preparedStatement.setUnicodeStream(parameterIndex, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}
}
