package cloud.apposs.cachex.storage;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.DbConfig;
import cloud.apposs.cachex.storage.elasticsearch.ElasticSearchBuilder;
import cloud.apposs.cachex.storage.hbase.HbaseBuilder;
import cloud.apposs.cachex.storage.jdbc.DbPool;
import cloud.apposs.cachex.storage.jdbc.MysqlBuilder;
import cloud.apposs.cachex.storage.jdbc.SqliteBuilder;
import cloud.apposs.util.StrUtil;

public class SqlBuilderFactory {
    /**
     * 判断是否为JDBC接口
     */
    public static boolean isJdbcDialect(String dialect) {
        if (StrUtil.isEmpty(dialect)) {
            return false;
        }
        dialect = dialect.toUpperCase();
        return dialect.equalsIgnoreCase(SqlBuilder.DIALECT_MYSQL) ||
                dialect.equalsIgnoreCase(SqlBuilder.DIALECT_ORACLE) ||
                dialect.equalsIgnoreCase(SqlBuilder.DIALECT_SQLITE);
    }

    /**
     * 创建SQL编译器，注意该方法内部可能创建了连接池，调用时必须只调用一次，单例模式
     */
    public static SqlBuilder getSqlBuilder(CacheXConfig config) throws Exception {
        if (config == null) {
            throw new IllegalArgumentException("config");
        }
        if (StrUtil.isEmpty(config.getDialect())) {
            throw new IllegalArgumentException("dialect");
        }

        String dialect = config.getDialect();
        if (isJdbcDialect(dialect)) {
            // JDBC模式下采用数据库连接池
            DbConfig dbCfg = config.getDbConfig();
            String driver = dbCfg.getDriverClass();
            String jdbcUrl = dbCfg.getJdbcUrl();
            if (StrUtil.isEmpty(jdbcUrl)) {
                throw new IllegalArgumentException("jdbc url cannot be null");
            }
            // 如果不指定驱动，则系统根据不同的数据库方言返回不同的驱动
            if (StrUtil.isEmpty(driver)) {
                dbCfg.setDriverClass(getJdbcDriver(dialect));
            }
            // 创建数据库连接池
            DbPool dbPool = new DbPool(dbCfg);
            if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_MYSQL)) {
                return new MysqlBuilder(dbPool).develop(config.isDevelop());
            } else if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_SQLITE)) {
                return new SqliteBuilder(dbPool).develop(config.isDevelop());
            }
        } else {
            if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_HBASE)) {
                return new HbaseBuilder(config);
            }
            if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_ELASTICSEARCH)) {
                return new ElasticSearchBuilder(config);
            }
        }
        return null;
    }

    /**
     * 根据不同的方言获取不同的数据库驱动
     */
    public static String getJdbcDriver(String dialect) {
        if (StrUtil.isEmpty(dialect)) {
            return null;
        }
        if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_MYSQL)) {
            return SqlBuilder.DRIVER_MYSQL;
        }
        if (dialect.equalsIgnoreCase(SqlBuilder.DIALECT_SQLITE)) {
            return SqlBuilder.DRIVER_SQLITE;
        }
        return null;
    }
}
