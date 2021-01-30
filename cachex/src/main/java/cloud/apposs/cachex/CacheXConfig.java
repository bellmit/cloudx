package cloud.apposs.cachex;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import cloud.apposs.cachex.memory.Cache;
import cloud.apposs.cachex.memory.jvm.CacheEvictionPolicyStrategy;
import cloud.apposs.cachex.storage.SqlBuilder;

/**
 * 数据源框架全局配置，业务可继承此方法，
 * 一个配置即一个数据源，内部对应是哪个DB数据源，哪个缓存类型
 */
public class CacheXConfig {
    /**
     * 是否为开发模式，
     * 开发模式下有几种情况：
     * 1、输出SQL执行语句
     */
    private boolean develop = false;

    /**
     * 是否要支持异步获取数据源数据，不异步获取则不起线程池节省资源
     */
    private boolean async = false;

    /**
     * 数据库方言，默认为MYSQL
     */
    private String dialect = SqlBuilder.DIALECT_MYSQL;

    /**
     * 缓存类型，默认为JVM内存
     */
    private String cache = Cache.CACHE_JVM;

    /**
     * 所有操作的字符，便于统一字符编码
     */
    private String charsetName = "utf-8";
    private Charset chrset = Charset.forName(charsetName);

    /**
     * 是否采用直接堆内存存储缓存
     */
    private boolean directBuffer = false;

    /**
     * 缓存数据是否采用异步写，即数据存入数据库之后由线路执行异步写入缓存
     */
    private boolean writeBehind = true;

    /**
     * 数据库连接池相关配置
     */
    private DbConfig dbConfig = new DbConfig();

    /**
     * HBase相关配置
     */
    private HbaseConfig hbaseConfig = new HbaseConfig();

    /**
     * ElasticSearch相关配置
     */
    private ElasticSearchConfig esConfig = new ElasticSearchConfig();

    /**
     * JVM缓存相关配置
     */
    private JvmConfig jvmConfig = new JvmConfig();

    /**
     * Redis缓存相关配置
     */
    private RedisConfig redisConfig = new RedisConfig();

    public boolean isDevelop() {
        return develop;
    }

    public void setDevelop(boolean develop) {
        this.develop = develop;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
        this.chrset = Charset.forName(charsetName);
    }

    public Charset getChrset() {
        return chrset;
    }

    public void setChrset(Charset chrset) {
        this.chrset = chrset;
    }

    public boolean isDirectBuffer() {
        return directBuffer;
    }

    public void setDirectBuffer(boolean directBuffer) {
        this.directBuffer = directBuffer;
    }

    public boolean isWriteBehind() {
        return writeBehind;
    }

    public void setWriteBehind(boolean writeBehind) {
        this.writeBehind = writeBehind;
    }

    public DbConfig getDbConfig() {
        return dbConfig;
    }

    public void setDbConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    public HbaseConfig getHbaseConfig() {
        return hbaseConfig;
    }

    public void setHbaseConfig(HbaseConfig hbaseConfig) {
        this.hbaseConfig = hbaseConfig;
    }

    public ElasticSearchConfig getEsConfig() {
        return esConfig;
    }

    public void setEsConfig(ElasticSearchConfig esConfig) {
        this.esConfig = esConfig;
    }

    public JvmConfig getJvmConfig() {
        return jvmConfig;
    }

    public void setJvmConfig(JvmConfig jvmConfig) {
        this.jvmConfig = jvmConfig;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    /**
     * JDBC数据库及数据库连接池相关配置
     */
    public static class DbConfig {
        /**
         * 数据库驱动类
         */
        private String driverClass;

        /**
         * 数据库URL连接
         */
        private String jdbcUrl;

        /**
         * 数据库用户名
         */
        private String username;

        /**
         * 数据库密码
         */
        private String password;

        /**
         * 连接池最小Connection连接数
         */
        private int minConnections = 12;

        /**
         * 连接池最大Connection连接数，包括空闲和忙碌的Connection连接数
         */
        private int maxConnections = Byte.MAX_VALUE;

        /**
         * 当连接池中的连接耗尽的时候系统每次自增的Connection连接数，默认为4
         */
        private int acquireIncrement = 4;

        /**
         * Connection连接存活时间，单位毫秒，
         * 在Connection连接大于{@link #minConnections}的情况下将被关闭并移除
         */
        private long aliveTime = 60000L;

        /**
         * Statement缓存，小于等于0时不缓存Statement
         */
        private int statementsCacheSize = -1;

        /**
         * 连接的自动提交模式
         */
        private Boolean autoCommit;

        /**
         * Connection是否处于只读模式
         */
        private Boolean readOnly;

        /**
         * 对象的事务隔离级别
         */
        private Integer transactionIsolation;

        /**
         * 在取得连接的同时是否校验连接的有效性，默认为true，
         * 注意，开启此功能对连接池的性能将有一定影响
         */
        private boolean testConnectionOnCheckout = true;

        /**
         * 在回收连接的同时是否校验连接的有效性，默认为false，
         * 注意，开启此功能对连接池的性能将有一定影响
         */
        private boolean testConnectionOnCheckin = false;

        /**
         * 是否开启数据库连接池操作跟踪
         */
        private boolean poolOperationWatch = true;

        public String getDriverClass() {
            return driverClass;
        }

        public String getJdbcUrl() {
            return jdbcUrl;
        }

        public void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public int getMinConnections() {
            return minConnections;
        }

        public void setMinConnections(int minConnections) {
            this.minConnections = minConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getAcquireIncrement() {
            return acquireIncrement;
        }

        public void setAcquireIncrement(int acquireIncrement) {
            this.acquireIncrement = acquireIncrement;
        }

        public long getAliveTime() {
            return aliveTime;
        }

        public void setAliveTime(long aliveTime) {
            this.aliveTime = aliveTime;
        }

        public int getStatementsCacheSize() {
            return statementsCacheSize;
        }

        public void setStatementsCacheSize(int statementsCacheSize) {
            this.statementsCacheSize = statementsCacheSize;
        }

        public Boolean getAutoCommit() {
            return autoCommit;
        }

        public void setAutoCommit(Boolean autoCommit) {
            this.autoCommit = autoCommit;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public Integer getTransactionIsolation() {
            return transactionIsolation;
        }

        public void setTransactionIsolation(Integer transactionIsolation) {
            this.transactionIsolation = transactionIsolation;
        }

        public boolean isTestConnectionOnCheckout() {
            return testConnectionOnCheckout;
        }

        public void setTestConnectionOnCheckout(boolean testConnectionOnCheckout) {
            this.testConnectionOnCheckout = testConnectionOnCheckout;
        }

        public boolean isTestConnectionOnCheckin() {
            return testConnectionOnCheckin;
        }

        public void setTestConnectionOnCheckin(boolean testConnectionOnCheckin) {
            this.testConnectionOnCheckin = testConnectionOnCheckin;
        }

        public boolean isPoolOperationWatch() {
            return poolOperationWatch;
        }

        public void setPoolOperationWatch(boolean poolOperationWatch) {
            this.poolOperationWatch = poolOperationWatch;
        }
    }

    /**
     * Hbase NoSql相关配置
     */
    public static class HbaseConfig {
        /**
         * Hbase主机地址
         */
        private String quorum;

        /**
         * ZooKeeper端口
         */
        private int zkPort = 2181;

        public String getQuorum() {
            return quorum;
        }

        public void setQuorum(String quorum) {
            this.quorum = quorum;
        }

        public int getZkPort() {
            return zkPort;
        }

        public void setZkPort(int zkPort) {
            this.zkPort = zkPort;
        }
    }

    /**
     * ElasticSearch NoSql相关配置
     */
    public static class ElasticSearchConfig {
        public static final int DEFAULT_ES_PORT = 9200;

        private String schema = "http";

        private String hosts;

        private int numberOfShards = 3;

        private int numberOfReplicas = 2;

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getHosts() {
            return hosts;
        }

        public void setHosts(String hosts) {
            this.hosts = hosts;
        }

        public int getNumberOfShards() {
            return numberOfShards;
        }

        public void setNumberOfShards(int numberOfShards) {
            this.numberOfShards = numberOfShards;
        }

        public int getNumberOfReplicas() {
            return numberOfReplicas;
        }

        public void setNumberOfReplicas(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }
    }

    /**
     * JVM缓存相关配置
     */
    public static class JvmConfig {
        /**
         * 缓存过期时间，单位毫秒，小于等于0为永为过期
         */
        private int expirationTime = -1;
        /**
         * 是否采用缓存过期时间随机，避免同一时间有大量缓存失效触发回收和数据落盘
         */
        private boolean expirationTimeRandom = false;
        /**
         * 缓存过期时间最小随机数，默认30分钟
         */
        private int expirationTimeRandomMin = 30 * 60 * 1000;
        /**
         * 缓存过期时间最大随机数，默认1小时
         */
        private int expirationTimeRandomMax = 60 * 60 * 1000;

        /**
         * 缓存过期定期检查间隔时间，默认为1分钟
         */
        private int expireCheckInterval = 60 * 1000;

        /**
         * 最多可以存放的缓存的条数，超过上限则会触发回收策略，-1为无限
         */
        private int maxElements = -1;

        /**
         * 最多可以存放的缓存的内存容量，单位字节(Byte)，超过上限则会触发回收策略，-1为无限
         */
        private long maxMemory = -1;

        /**
         * 缓存容器的并发级别，数据越多时需要设置的并发级别越高，否则获取Key数据会变成链表查找，性能会直线下降
         */
        private int concurrencyLevel = 1024;

        /**
         * 内存回收策略
         */
        private String evictionPolicy = CacheEvictionPolicyStrategy.CACHE_POLICY_LRU;

        public int getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(int expirationTime) {
            this.expirationTime = expirationTime;
        }

        public boolean isExpirationTimeRandom() {
            return expirationTimeRandom;
        }

        public void setExpirationTimeRandom(boolean expirationTimeRandom) {
            this.expirationTimeRandom = expirationTimeRandom;
        }

        public int getExpirationTimeRandomMin() {
            return expirationTimeRandomMin;
        }

        public void setExpirationTimeRandomMin(int expirationTimeRandomMin) {
            this.expirationTimeRandomMin = expirationTimeRandomMin;
        }

        public int getExpirationTimeRandomMax() {
            return expirationTimeRandomMax;
        }

        public void setExpirationTimeRandomMax(int expirationTimeRandomMax) {
            this.expirationTimeRandomMax = expirationTimeRandomMax;
        }

        public int getExpireCheckInterval() {
            return expireCheckInterval;
        }

        public void setExpireCheckInterval(int expireCheckInterval) {
            this.expireCheckInterval = expireCheckInterval;
        }

        public String getEvictionPolicy() {
            return evictionPolicy;
        }

        public void setEvictionPolicy(String evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
        }

        public int getMaxElements() {
            return maxElements;
        }

        public void setMaxElements(int maxElements) {
            this.maxElements = maxElements;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }

        public int getConcurrencyLevel() {
            return concurrencyLevel;
        }

        public void setConcurrencyLevel(int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
        }
    }

    /**
     * Redis缓存相关配置
     */
    public static class RedisConfig {
        /**
         * Redis单机缓存管理
         */
        public static final int REDIS_CACHE_SINGLE = 0;
        /**
         * Redis多机集群管理
         */
        public static final int REDIS_CACHE_CLUSTER = 1;
        /**
         * Codis代理分布管理
         */
        public static final int REDIS_CACHE_CODIS = 2;

        /**
         * Redis集群节点线程监听服务
         */
        public static final int REDIS_WATCHER_THREAD = 0;
        /**
         * Redis集群节点ZooKeeper监听服务
         */
        public static final int REDIS_WATCHER_ZOOKEEPER = 1;
        /**
         * Redis集群节点QConf监听服务
         */
        public static final int REDIS_WATCHER_QCONF = 2;

        /**
         * Redis缓存管理模式，有
         * {@link RedisConfig#REDIS_CACHE_SINGLE}、
         * {@link RedisConfig#REDIS_CACHE_CLUSTER}、
         * {@link RedisConfig#REDIS_CACHE_CODIS}
         */
        private int cacheType = REDIS_CACHE_SINGLE;

        /**
         * Redis节点监听服务配置
         */
        private int watcherType = REDIS_WATCHER_THREAD;

        /**
         * 连接池最小Connection连接数
         */
        private int minConnections = 12;

        /**
         * 连接池最大Connection连接数，包括空闲和忙碌的Connection连接数
         */
        private int maxConnections = Byte.MAX_VALUE;

        /**
         * 当连接池中的连接耗尽的时候系统每次自增的Connection连接数，默认为4
         */
        private int acquireIncrement = 4;

        /**
         * Redis连接超时时间
         */
        private int connectTimeout = 4 * 1000;

        /**
         * Redis接收数据超时时间
         */
        private int recvTimeout = 60 * 1000;

        /**
         * 在取得连接的同时是否校验连接的有效性，默认为true，
         * 注意，开启此功能对连接池的性能将有一定影响
         */
        private boolean testConnectionOnCheckout = true;

        /**
         * 在回收连接的同时是否校验连接的有效性，默认为false，
         * 注意，开启此功能对连接池的性能将有一定影响
         */
        private boolean testConnectionOnCheckin = false;

        /**
         * 是否开启连接池操作跟踪
         */
        private boolean poolOperationWatch = true;

        /**
         * Redis节点列表，可以是Redis单机、集群，也可以是Codis代理集群，
         * Redis单机模式下只用第一个节点
         */
        private List<String> serverNameList = new LinkedList<String>();
        private List<RedisServer> serverList = new LinkedList<RedisServer>();

        /**
         * 缓存过期时间，单位毫秒，小于等于0为永为过期
         */
        private int expirationTime = -1;
        /**
         * 是否采用缓存过期时间随机，避免同一时间有大量缓存失效触发回收和数据落盘
         */
        private boolean expirationTimeRandom = false;
        /**
         * 缓存过期时间最小随机数
         */
        private int expirationTimeRandomMin = 30 * 60 * 1000;
        /**
         * 缓存过期时间最大随机数
         */
        private int expirationTimeRandomMax = 60 * 60 * 1000;

        public int getCacheType() {
            return cacheType;
        }

        public void setCacheType(int cacheType) {
            this.cacheType = cacheType;
        }

        public int getWatcherType() {
            return watcherType;
        }

        public void setWatcherType(int watcherType) {
            this.watcherType = watcherType;
        }

        public List<String> getServerNameList() {
            return serverNameList;
        }

        public void setServerNameList(List<String> serverNameList) {
            if (serverNameList == null || serverNameList.isEmpty()) {
                return;
            }
            this.serverNameList = serverNameList;
            serverList.clear();
            for (String serverName : serverNameList) {
                String[] serverInfo = serverName.split(":");
                if (serverInfo.length != 2) {
                    continue;
                }
                String serverHost = serverInfo[0];
                int serverPort = Integer.parseInt(serverInfo[1]);
                addServer(new RedisServer(serverHost, serverPort));
            }
        }

        public List<RedisServer> getServerList() {
            return serverList;
        }

        public void addServer(RedisServer server) {
            serverList.add(server);
        }

        public int getMinConnections() {
            return minConnections;
        }

        public void setMinConnections(int minConnections) {
            this.minConnections = minConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getAcquireIncrement() {
            return acquireIncrement;
        }

        public void setAcquireIncrement(int acquireIncrement) {
            this.acquireIncrement = acquireIncrement;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getRecvTimeout() {
            return recvTimeout;
        }

        public void setRecvTimeout(int recvTimeout) {
            this.recvTimeout = recvTimeout;
        }

        public boolean isTestConnectionOnCheckout() {
            return testConnectionOnCheckout;
        }

        public void setTestConnectionOnCheckout(boolean testConnectionOnCheckout) {
            this.testConnectionOnCheckout = testConnectionOnCheckout;
        }

        public boolean isTestConnectionOnCheckin() {
            return testConnectionOnCheckin;
        }

        public void setTestConnectionOnCheckin(boolean testConnectionOnCheckin) {
            this.testConnectionOnCheckin = testConnectionOnCheckin;
        }

        public boolean isPoolOperationWatch() {
            return poolOperationWatch;
        }

        public void setPoolOperationWatch(boolean poolOperationWatch) {
            this.poolOperationWatch = poolOperationWatch;
        }

        public int getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(int expirationTime) {
            this.expirationTime = expirationTime;
        }

        public boolean isExpirationTimeRandom() {
            return expirationTimeRandom;
        }

        public void setExpirationTimeRandom(boolean expirationTimeRandom) {
            this.expirationTimeRandom = expirationTimeRandom;
        }

        public int getExpirationTimeRandomMin() {
            return expirationTimeRandomMin;
        }

        public void setExpirationTimeRandomMin(int expirationTimeRandomMin) {
            this.expirationTimeRandomMin = expirationTimeRandomMin;
        }

        public int getExpirationTimeRandomMax() {
            return expirationTimeRandomMax;
        }

        public void setExpirationTimeRandomMax(int expirationTimeRandomMax) {
            this.expirationTimeRandomMax = expirationTimeRandomMax;
        }

        public static class RedisServer {
            private final String host;

            private final int port;

            /**
             * 是否配置该服务为在线服务状态，false则不作为在线服务
             */
            private final boolean online;

            public RedisServer(String host, int port) {
                this(host, port, true);
            }

            public RedisServer(String host, int port, boolean online) {
                this.host = host;
                this.port = port;
                this.online = online;
            }

            public String getHost() {
                return host;
            }

            public int getPort() {
                return port;
            }

            public boolean isOnline() {
                return online;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof RedisServer)) {
                    return false;
                }
                RedisServer server = (RedisServer) obj;
                return host.equals(server.getHost()) && port == server.getPort();
            }

            @Override
            public int hashCode() {
                return (host + port).hashCode();
            }

            @Override
            public String toString() {
                StringBuilder info = new StringBuilder();
                info.append("[Host=").append(host);
                info.append(", Port=").append(port);
                info.append(", Online=").append(online);
                info.append("]");
                return info.toString();
            }
        }
    }
}
