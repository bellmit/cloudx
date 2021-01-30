package cloud.apposs.cachex.storage.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

/**
 * JDBC Connection包装类，所有的Connection操作都是通过此类再间接操作JDBC Connection
 */
public class ConnectionWrapper implements Connection {
    private final DbPool pool;

    /**
     * JDBC Connection
     */
    private final Connection connection;

    /**
     * 数据库URL连接
     */
    private final String url;

    /**
     * 数据库用户名
     */
    private final String username;

    /**
     * 数据库密码
     */
    private final String password;

    /**
     * Connection连接创建时间
     */
    private long connectionCreationTime = System.currentTimeMillis();

    /**
     * Connection连接空闲存活时间，小于/等于0为无限存活
     */
    private long aliveTime;

    /**
     * 当前Connection是否已经无效
     */
    private volatile boolean invalid = false;

    /**
     * 当前Connection是否被关闭，即被调用{@link #close()}方法
     * 此时真正Connection并未被真正关闭，只是逻辑意义上的关闭
     * 在下次从数据库连接池取出时仍然可用
     */
    private AtomicBoolean logicallyClosed = new AtomicBoolean(false);

    /**
     * 各Statement缓存
     */
    private StatementCache statementCache;
    private StatementCache preparedStatementCache;
    private StatementCache callableStatementCache;

    /**
     * 是否缓存Statement
     */
    private boolean statementCacheEnabled = false;

    private static HashSet<String> sqlStateDBFailureCodes;

    public ConnectionWrapper(DbPool pool, String url, String username, String password) throws SQLException {
        if (pool == null || StrUtil.isEmpty(url)
                || StrUtil.isEmpty(username) || StrUtil.isEmpty(password)) {
            throw new IllegalArgumentException();
        }

        this.pool = pool;
        this.url = url.trim();
        this.username = username.trim();
        this.password = password.trim();
        this.aliveTime = pool.getConfig().getAliveTime();
        this.connection = createRawConnection();

        int cacheSize = pool.getConfig().getStatementsCacheSize();
        if (cacheSize > 0) {
            this.statementCache = new StatementCache(cacheSize);
            this.preparedStatementCache = new StatementCache(cacheSize);
            this.callableStatementCache = new StatementCache(cacheSize);
            this.statementCacheEnabled = true;
        }
		
		/*
	 	From: http://publib.boulder.ibm.com/infocenter/db2luw/v8/index.jsp?topic=/com.ibm.db2.udb.doc/core/r0sttmsg.htm
	 	Table 7. Class Code 08: Connection Exception
		SQLSTATE Value	  
		Value	Meaning
		08001	The application requester is unable to establish the connection.
		08002	The connection already exists.
		08003	The connection does not exist.
		08004	The application server rejected establishment of the connection.
		08007	Transaction resolution unknown.
		08502	The CONNECT statement issued by an application process running with a SYNCPOINT of TWOPHASE has failed, because no transaction manager is available.
		08504	An error was encountered while processing the specified path rename configuration file.
		 */
        sqlStateDBFailureCodes = new HashSet<String>();
        sqlStateDBFailureCodes.add("08001");
        sqlStateDBFailureCodes.add("08007");
        sqlStateDBFailureCodes.add("08S01");
        sqlStateDBFailureCodes.add("57P01");
    }

    /**
     * 创建原始的未经包装的JDBC Connection
     */
    private Connection createRawConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, username, password);

        DbConfig config = pool.getConfig();
        if (config.getAutoCommit() != null) {
            connection.setAutoCommit(config.getAutoCommit());
        }
        if (config.getReadOnly() != null) {
            connection.setReadOnly(config.getReadOnly());
        }
        if (config.getTransactionIsolation() != null) {
            connection.setTransactionIsolation(config.getTransactionIsolation());
        }

        return connection;
    }

    /**
     * 获取原始的未经包装的JDBC Connection
     */
    protected Connection getRawConnection() {
        return connection;
    }

    /**
     * 关闭内部JDBC Connection
     */
    protected void internalClose() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * 判断该Connection连接是否过期
     *
     * @param currentTime 当前时间
     * @return 过期返回true
     */
    public boolean isExpired(long currentTime) {
        return aliveTime > 0 && currentTime - connectionCreationTime > aliveTime;
    }

    public boolean isInvalid() {
        return invalid;
    }

    /**
     * 激活Connection连接，在从数据库连接池取出Connection连接时调用此方法
     */
    protected void active() {
        this.logicallyClosed.set(false);
    }

    private void checkClosed() throws SQLException {
        if (this.logicallyClosed.get()) {
            throw new SQLException("Connection is closed!");
        }
    }

    /**
     * 根据抛出的异常来判断并标识当前Connection连接是否有效
     */
    protected SQLException markInvalid(SQLException e) {
        String state = e.getSQLState();
        if (state == null) {
            // 安全状态
            state = "08999";
        }

        // 致命错误，关闭所有连接
        if (sqlStateDBFailureCodes.contains(state)) {
            pool.getListenerSupport().firePoolInvalid(e);
            pool.terminateAllConnections();
        }

        char firstChar = state.charAt(0);
        if (state.equals("40001") || state.equals("HY000") ||
                state.startsWith("08") || (firstChar >= '5' && firstChar <= '9')) {
            this.invalid = true;
        }

        return e;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isStatementCacheEnabled() {
        return statementCacheEnabled;
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkClosed();
        try {
            connection.clearWarnings();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void close() throws SQLException {
        try {
            if (!logicallyClosed.get()) {
                logicallyClosed.set(true);
                pool.retriveConnection(this);
                if (pool.getConfig().isPoolOperationWatch()) {
                    pool.removeConnectionStackTrace(this);
                }
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void commit() throws SQLException {
        checkClosed();
        try {
            connection.commit();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements)
            throws SQLException {
        Array result = null;
        checkClosed();
        try {
            result = connection.createArrayOf(typeName, elements);
        } catch (SQLException e) {
            throw markInvalid(e);
        }

        return result;
    }

    @Override
    public Blob createBlob() throws SQLException {
        Blob result = null;
        checkClosed();
        try {
            result = connection.createBlob();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Clob createClob() throws SQLException {
        Clob result = null;
        checkClosed();
        try {
            result = connection.createClob();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public NClob createNClob() throws SQLException {
        NClob result = null;
        checkClosed();
        try {
            result = connection.createNClob();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        SQLXML result = null;
        checkClosed();
        try {
            result = connection.createSQLXML();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Statement createStatement() throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = StatementCache.CACHE_KEY_EMPTY;
                result = statementCache.get(cacheKey);
                if (result == null) {
                    result = new StatementWrapper(this, connection.createStatement(), null, cacheKey);
                    this.statementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new StatementWrapper(this, connection.createStatement());
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (this.statementCacheEnabled) {
                String cacheKey = statementCache.calculateCacheKey(resultSetType, resultSetConcurrency);
                result = statementCache.get(cacheKey);
                if (result == null) {
                    result = new StatementWrapper(this, connection.createStatement(resultSetType, resultSetConcurrency), null, cacheKey);
                    this.statementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new StatementWrapper(this, connection.createStatement(resultSetType, resultSetConcurrency));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (this.statementCacheEnabled) {
                String cacheKey = statementCache.calculateCacheKey(resultSetType, resultSetConcurrency, resultSetHoldability);
                result = statementCache.get(cacheKey);
                if (result == null) {
                    result = new StatementWrapper(this, connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), null, cacheKey);
                    this.statementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new StatementWrapper(this, connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes)
            throws SQLException {
        Struct result = null;
        checkClosed();
        try {
            result = connection.createStruct(typeName, attributes);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        checkClosed();
        try {
            connection.setSchema(schema);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public String getSchema() throws SQLException {
        String result = null;
        checkClosed();
        try {
            result = connection.getSchema();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        checkClosed();
        try {
            connection.abort(executor);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        checkClosed();
        try {
            connection.setNetworkTimeout(executor, milliseconds);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        int timeout = -1;
        checkClosed();
        try {
            timeout = connection.getNetworkTimeout();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return timeout;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        boolean result = false;
        checkClosed();
        try {
            result = connection.getAutoCommit();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public String getCatalog() throws SQLException {
        String result = null;
        checkClosed();
        try {
            result = connection.getCatalog();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        Properties result = null;
        checkClosed();
        try {
            result = connection.getClientInfo();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        String result = null;
        checkClosed();
        try {
            result = connection.getClientInfo(name);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public int getHoldability() throws SQLException {
        int result = 0;
        checkClosed();
        try {
            result = connection.getHoldability();
        } catch (SQLException e) {
            throw markInvalid(e);
        }

        return result;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        DatabaseMetaData result = null;
        checkClosed();
        try {
            result = connection.getMetaData();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        int result = 0;
        checkClosed();
        try {
            result = connection.getTransactionIsolation();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        Map<String, Class<?>> result = null;
        checkClosed();
        try {
            result = connection.getTypeMap();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        SQLWarning result = null;
        checkClosed();
        try {
            result = connection.getWarnings();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.logicallyClosed.get();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        boolean result = false;
        checkClosed();
        try {
            result = connection.isReadOnly();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        boolean result = false;
        checkClosed();
        try {
            result = connection.isValid(timeout);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        String result = null;
        checkClosed();
        try {
            result = connection.nativeSQL(sql);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = sql;
                result = callableStatementCache.get(cacheKey);
                if (result == null) {
                    result = new CallableStatementWrapper(this, connection.prepareCall(sql), sql, cacheKey);
                    this.preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new CallableStatementWrapper(this, connection.prepareCall(sql));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (CallableStatement) result;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency) throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = callableStatementCache.calculateCacheKey(sql, resultSetType, resultSetConcurrency);
                result = callableStatementCache.get(cacheKey);
                if (result == null) {
                    result = new CallableStatementWrapper(this, connection.prepareCall(sql, resultSetType, resultSetConcurrency), sql, cacheKey);
                    this.preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new CallableStatementWrapper(this, connection.prepareCall(sql, resultSetType, resultSetConcurrency));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (CallableStatement) result;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = callableStatementCache.
                        calculateCacheKey(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
                result = callableStatementCache.get(cacheKey);
                if (result == null) {
                    result = new CallableStatementWrapper(this, connection.
                            prepareCall(sql, resultSetType, resultSetConcurrency), sql, cacheKey);
                    this.preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new CallableStatementWrapper(this, connection.
                        prepareCall(sql, resultSetType, resultSetConcurrency));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (CallableStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = sql;
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(sql), sql, cacheKey);
                    preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.prepareStatement(sql));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = preparedStatementCache.calculateCacheKey(sql, autoGeneratedKeys);
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, autoGeneratedKeys), sql, cacheKey);
                    preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, autoGeneratedKeys));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = preparedStatementCache.calculateCacheKey(sql, columnIndexes);
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, columnIndexes), sql, cacheKey);
                    this.preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, columnIndexes));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = preparedStatementCache.calculateCacheKey(sql, columnNames);
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, columnNames), sql, cacheKey);
                    preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, columnNames));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency) throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = preparedStatementCache.calculateCacheKey(sql, resultSetType, resultSetConcurrency);
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, resultSetType, resultSetConcurrency), sql, cacheKey);
                    preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.prepareStatement(sql, resultSetType, resultSetConcurrency));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        StatementWrapper result = null;

        checkClosed();

        try {
            // 如果开启Statement缓存则从缓存中取出，如果不存在则新PreparedStatement并加入到缓存中
            if (statementCacheEnabled) {
                String cacheKey = preparedStatementCache.calculateCacheKey(
                        sql, resultSetType, resultSetConcurrency, resultSetHoldability);
                result = preparedStatementCache.get(cacheKey);
                if (result == null) {
                    result = new PreparedStatementWrapper(this, connection.prepareStatement(
                            sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, cacheKey);
                    preparedStatementCache.putIfAbsent(cacheKey, result);
                }
            } else {
                result = new PreparedStatementWrapper(this, connection.
                        prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
            }
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return (PreparedStatement) result;
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        checkClosed();
        try {
            connection.releaseSavepoint(savepoint);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void rollback() throws SQLException {
        checkClosed();
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        checkClosed();
        try {
            connection.rollback(savepoint);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkClosed();
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        checkClosed();
        try {
            connection.setCatalog(catalog);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        connection.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value)
            throws SQLClientInfoException {
        connection.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        checkClosed();
        try {
            connection.setHoldability(holdability);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkClosed();
        try {
            connection.setReadOnly(readOnly);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        checkClosed();
        Savepoint result = null;
        try {
            result = connection.setSavepoint();
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        checkClosed();
        Savepoint result = null;
        try {
            result = connection.setSavepoint(name);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
        return result;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        checkClosed();
        try {
            connection.setTransactionIsolation(level);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        checkClosed();
        try {
            connection.setTypeMap(map);
        } catch (SQLException e) {
            throw markInvalid(e);
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return connection.unwrap(iface);
    }

    @Override
    public String toString() {
        StringBuffer info = new StringBuffer();
        info.append("{");
        info.append("connectionCreationTime:");
        info.append(new SimpleDateFormat("HH:mm:ss").format(new Date(connectionCreationTime)));
        info.append(", ");
        info.append("aliveTime:").append(aliveTime).append(", ");
        info.append("invalid:").append(invalid);
        info.append("}");
        return info.toString();
    }
}
