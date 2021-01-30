package cloud.apposs.cachex.storage.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.Updater;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.cachex.storage.Metadata.Column;
import cloud.apposs.cachex.storage.Where.Condition;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

/**
 * SQLITE数据操作，
 * 注意因为SELECT/UPDATE不能使用同一连接的特性，该数据库不用连接池特性
 */
public class SqliteBuilder extends JdbcBuilder {
    public SqliteBuilder(DbPool dbPool) {
        super(dbPool);
    }

    @Override
    public String getDialect() {
        return DIALECT_SQLITE;
    }

    @Override
    public String generateQuerySql(String table, String primary, Object identity, Query query) {
        String field = Query.DEFAULT_FIELDS;
        if (query != null) {
            field = query.field();
        }
        StringBuilder sql = new StringBuilder(64);
        sql.append("SELECT ").append(field);
        sql.append(" FROM ").append(table);
        // 主键查询和条件查询互斥，因为主键查询就表示已经唯一值
        if (!StrUtil.isEmpty(primary) && identity != null) {
            // 主键查询
            sql.append(" WHERE ");
            sql.append("(").append(primary).append("=").append(identity).append(")");
        } else if (query != null && query.where() != null && !query.where().isEmpty()) {
            // 条件查询
            sql.append(" WHERE ");
            Where where = query.where();
            sql.append(doGenerateWhereSql(where));
        }
        // 分页查询
        if (query != null && query.pager() != null) {
            int start = query.pager().getStart();
            int limit = query.pager().getLimit();
            sql.append(" LIMIT ").append(start).append(", ").append(limit);
        }
        return sql.toString();
    }

    @Override
    public String generateDeleteSql(String table, String primary, Object identity) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("DELETE FROM ").append(table);
        // 条件范围缩小
        sql.append(" WHERE " + primary + "=?");
        return sql.toString();
    }

    @Override
    public String generateDeleteSql(String table, Entity entity) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("DELETE FROM ").append(table);
        // 条件范围缩小
        sql.append(" WHERE " + entity.getPrimary() + "=?");
        return sql.toString();
    }

    @Override
    public String generateDeleteSql(String table, Where where) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("DELETE FROM ").append(table);
        // WHERE条件匹配
        sql.append(" WHERE " + doGenerateWhereSql(where));
        return sql.toString();
    }

    @Override
    public String generateInertSql(String table, Entity entity) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("INSERT INTO ").append(table);
        // 字段更新
        int index = 0;
        Param fieldDatas = entity.getDatas();
        String fieldKeys = "";
        String fieldValues = "";
        int fieldTotal = fieldDatas.size();
        for (String key : fieldDatas.keySet()) {
            fieldKeys += key;
            fieldValues += "?";
            if (index < fieldTotal - 1) {
                fieldKeys += ", ";
                fieldValues += ", ";
            }
            index++;
        }
        sql.append(" (").append(fieldKeys).append(")");
        sql.append(" VALUES(").append(fieldValues).append(")");
        return sql.toString();
    }

    @Override
    public String generateUpdateSql(String table, Entity entity) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("UPDATE ").append(table);
        // 字段更新
        sql.append(" SET ");
        Param fieldDatas = entity.getDatas();
        int index = 0;
        int dataSize = fieldDatas.size();
        for (Entry<String, Object> entry : fieldDatas.entrySet()) {
            index++;
            if (entry.getValue() == null) {
                continue;
            }
            // 主键字段不更新
            if (entry.getKey().equalsIgnoreCase(entity.getPrimary())) {
                continue;
            }
            sql.append(entry.getKey() + "=?");
            if (index < dataSize) {
                sql.append(", ");
            }
        }
        // 条件范围缩小
        sql.append(" WHERE " + entity.getPrimary() + "=?");
        return sql.toString();
    }

    @Override
    public String generateUpdateSql(String table, Updater updater) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("UPDATE ").append(table);
        // 字段更新
        sql.append(" SET ");
        List<Updater.Data> fieldDatas = updater.getDataList();
        int dataSize = fieldDatas.size();
        for (int i = 0; i < dataSize; i++) {
            Updater.Data data = fieldDatas.get(i);
            String dataKey = data.getKey();
            String dataOp = data.getOperation();
            if (!StrUtil.isEmpty(dataOp)) {
                sql.append(dataKey + "=" + dataKey + dataOp + "?");
            } else {
                sql.append(dataKey + "=" + "?");
            }
            if (i < dataSize - 1) {
                sql.append(", ");
            }
        }
        // 条件范围缩小
        Where where = updater.where();
        sql.append(" WHERE " + doGenerateWhereSql(where));
        return sql.toString();
    }

    @Override
    public String generateDropSql(String table) {
        return "DROP TABLE IF EXISTS " + table;
    }

    @Override
    public String[] generateCreateSql(String table, Metadata metadata) {
        StringBuilder sql = new StringBuilder(128);
        sql.append("CREATE TABLE " + table + "(").append(CRLF);
        List<Column> columnList = metadata.getColumnList();
		List<String> sqlList = new LinkedList<String>();
        // 生成表字段
        String primaryColumn = null;
        for (int i = 0; i < columnList.size(); i++) {
            Column column = columnList.get(i);
            String columnName = column.getName();
            int columnType = column.getType();
            int columnLength = column.getLength();
            boolean columnPrimary = column.isPrimary();
            boolean columnNotNull = column.isNotNull();
            String columnDefaultVal = column.getDefaultVal();
            sql.append("    " + columnName + " ");
            if (columnPrimary) {
                sql.append("INTEGER PRIMARY KEY AUTOINCREMENT");
            } else {
                if (columnType == Metadata.COLUMN_TYPE_INT) {
                    sql.append("INT");
                } else if (columnType == Metadata.COLUMN_TYPE_STRING) {
                    sql.append("VARCHAR");
                } else if (columnType == Metadata.COLUMN_TYPE_LONG) {
                    sql.append("BIGINT");
                } else if (columnType == Metadata.COLUMN_TYPE_DATE) {
                    sql.append("DATETIME");
                }
                if (columnLength > 0) {
                    sql.append("(").append(columnLength).append(")");
                }
                if (columnNotNull) {
                    sql.append(" NOT NULL");
                } else {
                    sql.append(" DEFAULT NULL");
                }
                if (!StrUtil.isEmpty(columnDefaultVal)) {
                    sql.append(" DEFAULT '").append(columnDefaultVal).append("'");
                }
                if (columnPrimary) {
                    primaryColumn = columnName;
                }
            }
            if (i < columnList.size() - 1) {
                sql.append(",");
            }
            sql.append(CRLF);
        }
        sql.append(")");
        sqlList.add(sql.toString());
        // 生成索引
        List<Metadata.Index> indexList = metadata.getIndexList();
        if (indexList == null || indexList.isEmpty()) {
            StringBuilder keyAppend = new StringBuilder(32);
            String indexName = "index_" + primaryColumn;
            if (!StrUtil.isEmpty(primaryColumn)) {
                keyAppend.append("CREATE UNIQUE INDEX " + indexName + " ON ")
                        .append(table).append("(" + primaryColumn + ")")
                        .append(CRLF);
                sqlList.add(keyAppend.toString());
            }
        } else {
            for (Metadata.Index index : indexList) {
                boolean isPrimaryIndex = index.isPrimary();
                String indexKey = index.getIndex();
                String indexName = index.getName();
                StringBuilder keyAppend = new StringBuilder(32);
                if (isPrimaryIndex) {
                    // 唯一索引
                    keyAppend.append("CREATE UNIQUE INDEX " + indexName + " ON ")
                            .append(table).append(" (" + indexKey + ")");
                } else {
                    // 单列索引或者组合索引
                    keyAppend.append("CREATE INDEX " + indexName + " ON ")
                            .append(table).append(" (" + indexKey + ")");
                }
                sqlList.add(keyAppend.toString());
            }
        }
        String[] sqls = new String[sqlList.size()];
        sqlList.toArray(sqls);
        return sqls;
    }

    /**
     * SQLITE不能在一个连接里即用SELECT又用UPDATE更改，所以连接池也没什么效果，直接创建底层连接
     */
    @Override
    protected Connection doGetConnection() throws SQLException {
        return dbPool.getRawConnection();
    }

    /**
     * SQLITE不能在一个连接里即用SELECT又用UPDATE更改，所以连接池也没什么效果，直接关闭底层连接
     */
    @Override
    protected void doCloseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
        }
    }

    private String doGenerateWhereSql(Where where) {
        StringBuilder sql = new StringBuilder(32);
        List<Condition> conditionList = where.getConditionList();
        int index = 0;
        for (Condition condition : conditionList) {
            if (index != 0) {
                if (condition.isAnd()) {
                    sql.append(" AND ");
                } else {
                    sql.append(" OR ");
                }
            }
            Where whereNext = condition.getWhere();
            if (whereNext != null && !whereNext.isEmpty()) {
                sql.append("(").append(doGenerateWhereSql(whereNext)).append(")");
            } else {
                String key = condition.getKey();
                String operation = condition.getOperation();
                Object value = condition.getValue();
                ConditionBuilder conditionBuilder = conditionBuilders.get(operation);
                if (conditionBuilder != null) {
                    sql.append(conditionBuilder.operationBuild(key, value));
                } else {
                    sql.append("(" + key + " " + operation + " ?)");
                }
            }
            index++;
        }
        return sql.toString();
    }
}
