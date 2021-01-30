package cloud.apposs.cachex.storage.jdbc;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.cachex.storage.Updater;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.cachex.storage.Where.Condition;
import cloud.apposs.util.Param;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Parser;
import cloud.apposs.util.Ref;
import cloud.apposs.util.StrUtil;
import cloud.apposs.logger.Logger;

/**
 * JDBC数据增、删、改、查操作，
 * 采用数据库连接池建立连接请求
 */
public abstract class JdbcBuilder implements SqlBuilder {
    public static final String CRLF = "\r\n";

    /**
     * 将JAVA数据类型转换为SQL数据类型的转换器
     */
    protected static final Map<Class<?>, StatementBuilder> statementBuilders =
            new HashMap<Class<?>, StatementBuilder>();
    static {
        statementBuilders.put(int.class, new IntStatementBuilder());
        statementBuilders.put(Integer.class, new IntStatementBuilder());
        statementBuilders.put(long.class, new LongStatementBuilder());
        statementBuilders.put(Long.class, new LongStatementBuilder());
        statementBuilders.put(float.class, new FloatStatementBuilder());
        statementBuilders.put(Float.class, new FloatStatementBuilder());
        statementBuilders.put(double.class, new DoubleStatementBuilder());
        statementBuilders.put(Double.class, new DoubleStatementBuilder());
        statementBuilders.put(short.class, new ShortStatementBuilder());
        statementBuilders.put(Short.class, new ShortStatementBuilder());
        statementBuilders.put(String.class, new StringStatementBuilder());
        statementBuilders.put(List.class, new ListStatementBuilder());
        statementBuilders.put(ByteBuffer.class, new ByteBufferStatementBuilder());
        statementBuilders.put(byte.class, new BytesStatementBuilder());
        statementBuilders.put(GregorianCalendar.class, new CalendarStatementBuilder());
        statementBuilders.put(Calendar.class, new CalendarStatementBuilder());
    }

    /**
     * 查询条件转换器
     */
    protected static final Map<String, ConditionBuilder> conditionBuilders =
            new HashMap<String, ConditionBuilder>();
    static {
        conditionBuilders.put(Where.LK, new LikeBuilder());
        conditionBuilders.put(Where.NL, new LikeBuilder());
        conditionBuilders.put(Where.IN, new InBuilder());
    }

    /**
     * 内部数据库连接池
     */
    protected final DbPool dbPool;

    /**
     * 开发模式下输出执行的SQL语句
     */
    protected boolean develop = false;

    public JdbcBuilder(DbPool dbPool) {
        this.dbPool = dbPool;
    }

    public JdbcBuilder develop(boolean develop) {
        this.develop = develop;
        return this;
    }

    @Override
    public Entity select(String table, String primary, Object identity,
                         ProtoSchema schema, Query query) throws SQLException {
        if (query == null) {
            query = new Query().limit(0, 1);
        } else {
            query.limit(0, 1);
        }
        String sql = generateQuerySql(table, primary, identity, query);
        return doExecuteSelect(sql, primary, query);
    }

    @Override
    public List<Entity> query(String table, String primary,
                              ProtoSchema schema, Query query) throws SQLException {
        String sql = generateQuerySql(table, primary, null, query);
        return doExecuteQuery(sql, primary, query);
    }

    @Override
    public int update(String table, Entity entity, ProtoSchema schema) throws SQLException {
        String sql = generateUpdateSql(table, entity);
        return doExecuteUpdate(sql, entity);
    }

    @Override
    public int update(String table, List<Entity> entities, ProtoSchema schema) throws SQLException {
        if (entities.isEmpty()) {
            return 0;
        }
        String sql = generateUpdateSql(table, entities.get(0));
        return doExecuteUpdateBatch(sql, entities);
    }

    @Override
    public int update(String table, Updater updater) throws SQLException {
        String sql = generateUpdateSql(table, updater);
        return doExecuteUpdate(sql, updater);
    }

    @Override
    public int insert(String table, Entity entity,
                      ProtoSchema schema, Ref<Object> idRef) throws SQLException {
        String sql = generateInertSql(table, entity);
        return doExecuteInsert(sql, entity, idRef);
    }

    @Override
    public int insert(String table, List<Entity> entities,
                      ProtoSchema schema, List<Object> idList) throws SQLException {
        if (entities.isEmpty()) {
            return 0;
        }
        String sql = generateInertSql(table, entities.get(0));
        return doExecuteInsertBatch(sql, entities, idList);
    }

    @Override
    public int delete(String table, String primary, Object identity) throws SQLException {
        String sql = generateDeleteSql(table, primary, identity);
        return doExecuteUpdate(sql, primary, identity);
    }

    @Override
    public int delete(String table, Entity entity) throws SQLException {
        String sql = generateDeleteSql(table, entity);
        return doExecuteUpdate(sql, entity);
    }

    @Override
    public int delete(String table, List<Entity> entities) throws SQLException {
        if (entities.isEmpty()) {
            return 0;
        }
        String sql = generateDeleteSql(table, entities.get(0));
        return doExecuteUpdateBatch(sql, entities);
    }

    @Override
    public int delete(String table, Where where) throws SQLException {
        String sql = generateDeleteSql(table, where);
        return doExecuteUpdate(sql, where);
    }

    @Override
    public Entity executeSelect(String sql, String primary, Object... args) throws SQLException {
        return doExecuteSelect(sql, primary, null);
    }

    @Override
    public List<Entity> executeQuery(String sql, String primary, Object... args) throws SQLException {
        return doExecuteQuery(sql, primary, null);
    }

    @Override
    public int executeUpdate(String sql, Object... args) throws SQLException {
        return doExecuteUpdate(sql, (Updater) null);
    }

    @Override
    public boolean create(Metadata metadata, boolean dropIfExist) throws SQLException {
        String table = metadata.getTable();
        if (dropIfExist && exist(metadata)) {
            String dropSql = generateDropSql(table);
            executeUpdate(dropSql);
        }
        String[] sqls = generateCreateSql(table, metadata);
        if (sqls == null || sqls.length <= 0) {
            throw new SQLException("generate '" + table + "' create sql failure");
        }
        return doExecuteUpdateBatch(sqls) >= 0;
    }

    @Override
    public boolean exist(Metadata metadata) throws SQLException {
        Connection connection = null;
        try {
            String table = metadata.getTable();
            connection = doGetConnection();
            ResultSet rs = connection.getMetaData().getTables(null, null, table, null);
            return rs.next();
        } finally {
            doCloseConnection(connection);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (dbPool != null && dbPool.isRunning()) {
            dbPool.shutdown();
        }
    }

    private Entity doExecuteSelect(String sql, String primary, Query query) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Query Sql: %s", sql);
            }
            Entity entity = new Entity(primary);
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            if (query != null) {
                buildStatement(statement, 1, query.where());
            }
            ResultSet result = statement.executeQuery();
            ResultSetMetaData metadata = result.getMetaData();
            int columns = metadata.getColumnCount();

            if (result.next()) {
                // 遍历数据库行数据字段并设置到实体类中
                for (int i = 1; i <= columns; i++) {
                    Object value = result.getObject(i);
                    // 数据库自身特殊对象类型需要进行转换以匹配框架类型
                    if (value instanceof Timestamp) {
                        entity.setCalendar(metadata.getColumnLabel(i), Parser.parseCalendar((Timestamp) value));
                    } else {
                        entity.setObject(metadata.getColumnLabel(i), value);
                    }
                }
                // 设置实体类主键便于之后做更新/删除操作
                if (!StrUtil.isEmpty(primary)) {
                    entity.setIdentity(entity.getObject(primary));
                }
                return entity;
            }
        } finally {
            doCloseConnection(connection);
        }
        return null;
    }

    private List<Entity> doExecuteQuery(String sql,
                                        String primary, Query query) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Query Sql: %s", sql);
            }
            List<Entity> entities = new LinkedList<Entity>();
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            if (query != null) {
                buildStatement(statement, 1, query.where());
            }
            ResultSet result = statement.executeQuery();
            ResultSetMetaData metadata = result.getMetaData();
            int columns = metadata.getColumnCount();

            while (result.next()) {
                Entity entity = new Entity(primary);
                for (int i = 1; i <= columns; i++) {
                    int type = metadata.getColumnType(i);
                    Object value = result.getObject(i);
                    if (type == Types.TIMESTAMP) {
                        entity.setCalendar(metadata.getColumnLabel(i), Parser.parseCalendar((Timestamp) value));
                    } else if (type == Types.TIME) {
                        entity.setCalendar(metadata.getColumnLabel(i), Parser.parseCalendar((Timestamp) value));
                    }  else if (type == Types.DATE) {
                        // mysql为Data类型，sqlite为Long类型
                        if (value instanceof Long) {
                            entity.setCalendar(metadata.getColumnLabel(i), Parser.parseCalendar((Long) value));
                        } else {
                            entity.setCalendar(metadata.getColumnLabel(i), Parser.parseCalendar((java.sql.Date) value));
                        }
                    } else {
                        entity.setObject(metadata.getColumnLabel(i), value);
                    }
                }
                entities.add(entity);
            }
            return entities;
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdate(String sql, Updater updater) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Update Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            if (updater != null) {
                List<Updater.Data> dataList = updater.getDataList();
                int index = 1;
                for (int i = 0; i < dataList.size(); i++) {
                    Updater.Data data = dataList.get(i);

                    Object value = data.getValue();
                    StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                    index = stmtBuilder.build(statement, index, value);
                    index++;
                }
                buildStatement(statement, index, updater.where());
            }

            return statement.executeUpdate();
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdate(String sql, Where where) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Update Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            buildStatement(statement, 1, where);

            return statement.executeUpdate();
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdate(String sql, String primary, Object identity) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Update Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            Where where = new Where(primary, Where.EQ, identity);
            buildStatement(statement, 1, where);

            return statement.executeUpdate();
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdate(String sql, Entity entity) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Update Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            if (entity != null) {
                Param dataList = entity.getDatas();
                int index = 1;
                for (Entry<String, Object> entry : dataList.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    // Entity主键字段不更新
                    if (key.equalsIgnoreCase(entity.getPrimary())) {
                        continue;
                    }
                    StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                    index = stmtBuilder.build(statement, index, value);
                    index++;
                }
                Where where = new Where(entity.getPrimary(), Where.EQ, entity.getIdentity());
                buildStatement(statement, index, where);
            }

            return statement.executeUpdate();
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdateBatch(String sql, List<Entity> entities) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Update Batch Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            for (Entity entity : entities) {
                Param dataList = entity.getDatas();
                int index = 1;
                for (Entry<String, Object> entry : dataList.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    // Entity主键字段不更新
                    if (key.equalsIgnoreCase(entity.getPrimary())) {
                        continue;
                    }
                    StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                    index = stmtBuilder.build(statement, index, value);
                    index++;
                }
                Where where = new Where(entity.getPrimary(), Where.EQ, entity.getIdentity());
                buildStatement(statement, index, where);
                statement.addBatch();
            }

            try {
                int result = statement.executeBatch().length;
                statement.clearBatch();
                return result;
            } finally {
                statement.close();
            }
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteUpdateBatch(String[] sqls) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                for (int i = 0; i < sqls.length; i++) {
                    String sql = sqls[i];
                    Logger.info("Execute Update Batch Sql: %s", sql);
                }
            }
            connection = doGetConnection();
            Statement statement = connection.createStatement();
            for (int i = 0; i < sqls.length; i++) {
                String sql = sqls[i];
                statement.addBatch(sql);
            }

            try {
                int result = statement.executeBatch().length;
                statement.clearBatch();
                return result;
            } finally {
                statement.close();
            }
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteInsert(String sql, Entity entity, Ref<Object> idRef) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Insert Sql: %s", sql);
            }
            connection = doGetConnection();
            PreparedStatement statement = null;
            if (idRef != null) {
                statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else {
                statement = connection.prepareStatement(sql);
            }
            if (entity != null) {
                Param dataList = entity.getDatas();
                int index = 1;
                for (Object value : dataList.values()) {
                    StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                    if (stmtBuilder == null) {
                        throw new SQLException("No matched statement builder for " + value.getClass());
                    }
                    index = stmtBuilder.build(statement, index, value);
                    index++;
                }
            }

            int count = statement.executeUpdate();
            if (idRef != null) {
                ResultSet resultSet = statement.getGeneratedKeys();
                try {
                    if (resultSet != null && resultSet.next()) {
                        idRef.value(resultSet.getObject(1));
                    }
                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            return count;
        } finally {
            doCloseConnection(connection);
        }
    }

    private int doExecuteInsertBatch(String sql, List<Entity> entities, List<Object> idList) throws SQLException {
        Connection connection = null;
        try {
            if (develop) {
                Logger.info("Execute Insert Batch Sql: %s", sql);
            }
            connection = doGetConnection();

            PreparedStatement statement = null;
            if (idList != null) {
                statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else {
                statement = connection.prepareStatement(sql);
            }
            for (Entity entity : entities) {
                Param dataList = entity.getDatas();
                int index = 1;
                for (Object value : dataList.values()) {
                    StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                    index = stmtBuilder.build(statement, index, value);
                    index++;
                }
                statement.addBatch();
            }

            int count = statement.executeBatch().length;
            if (idList != null) {
                ResultSet resultSet = statement.getGeneratedKeys();
                try {
                    if (resultSet != null) {
                        while (resultSet.next()) {
                            idList.add(resultSet.getObject(1));
                        }
                    }
                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            }
            return count;
        } finally {
            doCloseConnection(connection);
        }
    }

    protected Connection doGetConnection() throws SQLException {
        return dbPool.getConnection();
    }

    protected void doCloseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
        }
    }

    private static int buildStatement(PreparedStatement statement,
                                      int index, Where where) throws SQLException {
        if (where == null) {
            return index;
        }
        List<Condition> conditionList = where.getConditionList();
        for (Condition condition : conditionList) {
            if (condition.getWhere() != null) {
                // 查询条件嵌套，例如WHERER ID = 1 AND (CLASS = 1 OR CLASS = 2)
                index = buildStatement(statement, index, condition.getWhere());
            } else {
                String operation = condition.getOperation();
                Object value = condition.getValue();
                ConditionBuilder valueBuilder = conditionBuilders.get(operation);
                if (valueBuilder != null) {
                    value = valueBuilder.valueBuild(value);
                }
                StatementBuilder stmtBuilder = doGetStatementBuilder(value.getClass());
                index = stmtBuilder.build(statement, index, value);
                index++;
            }
        }
        return index;
    }

    private static StatementBuilder doGetStatementBuilder(Class<?> clazz) {
        if (clazz.isArray()) {
            Class<?> elementType = clazz.getComponentType();
            if (elementType.isAssignableFrom(byte.class)) {
                return statementBuilders.get(byte.class);
            }
            return statementBuilders.get(List.class);
        } else if (ByteBuffer.class.isAssignableFrom(clazz)) {
            return statementBuilders.get(ByteBuffer.class);
        }
        return statementBuilders.get(clazz);
    }

    /**
     * 查询条件自定义解析
     */
    public static interface ConditionBuilder {
        /**
         * 解析PreparedStatement.setXXX(value)对应的值，
         * 一般直接返回即可，但像LIKE等操作符需要返回%VALUE%以便于设置值
         */
        Object valueBuild(Object value);

        /**
         * 解析查询条件拼成SQL的字符串，
         * 例如LIKE操作符，生成的SQL语句为XXX LIKE ?
         */
        String operationBuild(String key, Object value);
    }

    public static class LikeBuilder implements ConditionBuilder {
        @Override
        public Object valueBuild(Object value) {
            return "%" + value + "%";
        }

        @Override
        public String operationBuild(String key, Object value) {
            return "(" + key + " LIKE ?)";
        }
    }

    public static class InBuilder implements ConditionBuilder {
        @Override
        public Object valueBuild(Object value) {
            return value;
        }

        @Override
        public String operationBuild(String key, Object value) {
            if (value.getClass().isAssignableFrom(List.class)) {
                @SuppressWarnings("unchecked")
                List<Object> inList = (List<Object>) value;
                int inSize = inList.size();
                StringBuilder inStr = new StringBuilder();
                for (int i = 0; i < inSize; i++) {
                    inStr.append("?");
                    if (i < inSize - 1) {
                        inStr.append(" , ");
                    }
                }
                return "(" + key + " IN (" + inStr + "))";
            } else if (value.getClass().isArray()) {
                Object[] array = (Object[]) value;
                StringBuilder inStr = new StringBuilder();
                for (int i = 0; i < array.length; i++) {
                    inStr.append("?");
                    if (i < array.length - 1) {
                        inStr.append(" , ");
                    }
                }
                return "(" + key + " IN (" + inStr + "))";
            } else {
                return "false";
            }
        }
    }

    /**
     * PreparedStatement属性值注入，不同的属性值类型不同其调用PreparedStatement设置的方法也不一样，
     * 例如Integer是调用PreparedStatement.setInt();
     */
    public static interface StatementBuilder {
        int build(PreparedStatement statement,
                  int index, Object value) throws SQLException;
    }

    public static class IntStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setInt(index, (Integer) value);
            return index;
        }
    }

    public static class LongStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setLong(index, (Long) value);
            return index;
        }
    }

    public static class FloatStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setFloat(index, (Float) value);
            return index;
        }
    }

    public static class DoubleStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setDouble(index, (Double) value);
            return index;
        }
    }

    public static class ShortStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setShort(index, (Short) value);
            return index;
        }
    }

    public static class StringStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            statement.setString(index, (String) value);
            return index;
        }
    }

    public static class ListStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            Class<?> type = value.getClass();
            if (type.isAssignableFrom(List.class)) {
                @SuppressWarnings("unchecked")
                List<Object> inList = (List<Object>) value;
                for (int i = 0; i < inList.size(); i++) {
                    Object inValue = inList.get(i);
                    StatementBuilder stmtBuilder = statementBuilders.get(inValue.getClass());
                    index = stmtBuilder.build(statement, index, inValue);
                }
            } else if (type.isArray()) {
                Object[] inList = (Object[]) value;
                for (int i = 0; i < inList.length; i++) {
                    Object inValue = inList[i];
                    StatementBuilder stmtBuilder = statementBuilders.get(inValue.getClass());
                    index = stmtBuilder.build(statement, index, inValue);
                    index++;
                }
            }
            return index;
        }
    }

    public static class ByteBufferStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            ByteBuffer buffer = (ByteBuffer) value;
            int size = buffer.limit();
            byte[] bytes = new byte[size];
            for (int i = 0; i < size; ++i) {
                bytes[i] = buffer.get(i);
            }
            statement.setBytes(index, bytes);
            return index;
        }
    }

    public static class BytesStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            byte[] bytes = (byte[]) value;
            statement.setBytes(index, bytes);
            return index;
        }
    }

    public static class CalendarStatementBuilder implements StatementBuilder {
        @Override
        public int build(PreparedStatement statement,
                         int index, Object value) throws SQLException {
            Calendar calendar = (Calendar) value;
            statement.setTimestamp(index, Parser.parseTimestamp(calendar));
            return index;
        }
    }

    /**
     * 生成数据查询语句
     *
     * @param table    查询表格
     * @param primary  查询主键字段名称，可为空
     * @param identity 查询主键值，需要和primary结合使用，可为空
     * @param query    查询条件，可为空
     * @return SQL查询语句
     */
    public abstract String generateQuerySql(String table, String primary, Object identity, Query query);

    /**
     * 生成数据更新语句
     */
    public abstract String generateUpdateSql(String table, Entity entity);

    public abstract String generateUpdateSql(String table, Updater updater);

    /**
     * 生成数据库插入语句
     */
    public abstract String generateInertSql(String table, Entity entity);

    /**
     * 生成数据库删除语句
     */
    public abstract String generateDeleteSql(String table, String primary, Object identity);

    public abstract String generateDeleteSql(String table, Entity entity);

    public abstract String generateDeleteSql(String table, Where where);

    /**
     * 生成数据库表删除语句
     */
    public abstract String generateDropSql(String table);

    /**
     * 生成数据库表创建语句
     */
    public abstract String[] generateCreateSql(String table, Metadata metadata);
}
