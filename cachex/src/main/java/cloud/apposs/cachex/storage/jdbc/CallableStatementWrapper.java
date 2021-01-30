package cloud.apposs.cachex.storage.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
	private CallableStatement callableStatement;
	
	public CallableStatementWrapper(ConnectionWrapper connection, CallableStatement callableStatement) {
		super(connection, callableStatement);
		this.callableStatement = callableStatement;
	}
	
	public CallableStatementWrapper(ConnectionWrapper connection, CallableStatement callableStatement, String sql,
			String cacheKey) {
		super(connection, callableStatement, sql, cacheKey);
		this.callableStatement = callableStatement;
	}

	@Override
	public Array getArray(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getArray(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Array getArray(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getArray(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBigDecimal(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBigDecimal(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	@Deprecated
	public BigDecimal getBigDecimal(int parameterIndex, int scale)
			throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBigDecimal(parameterIndex, scale);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Blob getBlob(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBlob(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Blob getBlob(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBlob(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBoolean(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public boolean getBoolean(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBoolean(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public byte getByte(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getByte(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public byte getByte(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getByte(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBytes(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public byte[] getBytes(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getBytes(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getCharacterStream(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getCharacterStream(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Clob getClob(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getClob(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Clob getClob(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getClob(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Date getDate(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDate(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Date getDate(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDate(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDate(parameterIndex, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDate(parameterName, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public double getDouble(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDouble(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public double getDouble(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getDouble(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public float getFloat(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getFloat(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public float getFloat(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getFloat(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public int getInt(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getInt(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public int getInt(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getInt(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public long getLong(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getLong(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public long getLong(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getLong(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNCharacterStream(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNCharacterStream(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public NClob getNClob(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNClob(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public NClob getNClob(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNClob(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public String getNString(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNString(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public String getNString(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getNString(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Object getObject(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Object getObject(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map)
			throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterIndex, map);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map)
			throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterName, map);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Ref getRef(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getRef(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Ref getRef(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getRef(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public RowId getRowId(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getRowId(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public RowId getRowId(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getRowId(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getSQLXML(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getSQLXML(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public short getShort(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getShort(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public short getShort(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getShort(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public String getString(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getString(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public String getString(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getString(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Time getTime(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTime(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Time getTime(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTime(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTime(parameterIndex, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTime(parameterName, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTimestamp(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTimestamp(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTimestamp(parameterIndex, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			return callableStatement.getTimestamp(parameterName, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public URL getURL(int parameterIndex) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getURL(parameterIndex);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public URL getURL(String parameterName) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getURL(parameterName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterIndex, type);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		checkClosed();
		try {
			return callableStatement.getObject(parameterName, type);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterIndex, sqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterName, sqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterIndex, sqlType, scale);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType,
			String typeName) throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterIndex, sqlType, typeName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType,
			int scale) throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterName, sqlType, scale);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType,
			String typeName) throws SQLException {
		checkClosed();
		try {
			callableStatement.registerOutParameter(parameterName, sqlType, typeName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setAsciiStream(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, int length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setAsciiStream(parameterName, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, long length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setAsciiStream(parameterName, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal x)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setBigDecimal(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setBinaryStream(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, int length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setBinaryStream(parameterName, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setBinaryStream(parameterName, x, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setBlob(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setBlob(parameterName, inputStream);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream,
			long length) throws SQLException {
		checkClosed();
		try {
			callableStatement.setBlob(parameterName, inputStream, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setBoolean(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setByte(String parameterName, byte x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setByte(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setBytes(String parameterName, byte[] x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setBytes(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setCharacterStream(parameterName, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader,
			int length) throws SQLException {
		checkClosed();
		try {
			callableStatement.setCharacterStream(parameterName, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader,
			long length) throws SQLException {
		checkClosed();
		try {
			callableStatement.setCharacterStream(parameterName, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(String parameterName, Clob x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setClob(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(String parameterName, Reader reader)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setClob(parameterName, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setClob(String parameterName, Reader reader, long length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setClob(parameterName, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDate(String parameterName, Date x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setDate(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDate(String parameterName, Date x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setDate(parameterName, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setDouble(String parameterName, double x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setDouble(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setFloat(String parameterName, float x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setFloat(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setInt(String parameterName, int x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setInt(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setLong(String parameterName, long x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setLong(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setNCharacterStream(parameterName, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value,
			long length) throws SQLException {
		checkClosed();
		try {
			callableStatement.setNCharacterStream(parameterName, value, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException {
		checkClosed();
		try {
			callableStatement.setNClob(parameterName, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(String parameterName, Reader reader)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setNClob(parameterName, reader);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNClob(String parameterName, Reader reader, long length)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setNClob(parameterName, reader, length);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNString(String parameterName, String value)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setNString(parameterName, value);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		checkClosed();
		try {
			callableStatement.setNull(parameterName, sqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setNull(parameterName, sqlType, typeName);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(String parameterName, Object x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setObject(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setObject(parameterName, x, targetSqlType);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType,
			int scale) throws SQLException {
		checkClosed();
		try {
			callableStatement.setObject(parameterName, x, targetSqlType, scale);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setRowId(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setSQLXML(parameterName, xmlObject);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setShort(String parameterName, short x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setShort(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setString(String parameterName, String x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setString(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTime(String parameterName, Time x) throws SQLException {
		checkClosed();
		try {
			callableStatement.setTime(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTime(String parameterName, Time x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setTime(parameterName, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setTimestamp(parameterName, x);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
			throws SQLException {
		checkClosed();
		try {
			callableStatement.setTimestamp(parameterName, x, cal);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public void setURL(String parameterName, URL val) throws SQLException {
		checkClosed();
		try {
			callableStatement.setURL(parameterName, val);
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}

	@Override
	public boolean wasNull() throws SQLException {
		checkClosed();
		try {
			return callableStatement.wasNull();
		} catch (SQLException e) {
			throw this.connection.markInvalid(e);
		}
	}
}
