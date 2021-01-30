package cloud.apposs.bootor;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.configure.Optional;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.server.http.HttpServerConfig;

import java.util.List;
import java.util.Map;

@Component
public class BootorConfig extends HttpServerConfig {
    /**
     * 扫描基础包，必须配置，框架会自动扫描Action注解类
     */
    protected String basePackage;

    /**
     * 是否开启权限注解拦截验证
     */
    protected boolean authDriven = false;

    /**
     * 是否开启请求监控
     */
    protected boolean monitorActive = false;

    /**
     * 是否输出系统信息
     */
    protected boolean showSysInfo = true;

    /**
     * 是否输出请求日志
     */
    protected boolean httpLogEnable = true;

    /**
     * 请求日志输出格式
     */
    protected String httpLogFormat;

    /**
     * 服务发现异步请求组件
     */
    protected OkHttpConfig okHttpConfig;

    /**
     * 业务自定义配置
     */
    protected Object options;

    /**
     * 服务注册相关配置
     */
    protected RegistryConfig registryConfig;

    /**
     * 数据源相关配置，支持多数据源配置，适用场景：
     * 1、固定数据存储用mysql源存储，文本存储用es源存储，便于文档可通过ES快速检索
     * 2、主从数据库读写分离，写用主库，读用从库，减少数据库压力，提升读性能
     */
    protected Map<String, ResourceConfig> resources;

    /**
     * 限流规则列表
     */
    protected List<GuardRule> rules;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public boolean isAuthDriven() {
        return authDriven;
    }

    public void setAuthDriven(boolean authDriven) {
        this.authDriven = authDriven;
    }

    public boolean isMonitorActive() {
        return monitorActive;
    }

    public void setMonitorActive(boolean monitorActive) {
        this.monitorActive = monitorActive;
    }

    public boolean isShowSysInfo() {
        return showSysInfo;
    }

    public void setShowSysInfo(boolean showSysInfo) {
        this.showSysInfo = showSysInfo;
    }

    public boolean isHttpLogEnable() {
        return httpLogEnable;
    }

    public void setHttpLogEnable(boolean httpLogEnable) {
        this.httpLogEnable = httpLogEnable;
    }

    public String getHttpLogFormat() {
        return httpLogFormat;
    }

    public void setHttpLogFormat(String httpLogFormat) {
        this.httpLogFormat = httpLogFormat;
    }

    public OkHttpConfig getOkHttpConfig() {
        return okHttpConfig;
    }

    public void setOkHttpConfig(OkHttpConfig okHttpConfig) {
        this.okHttpConfig = okHttpConfig;
    }

    public List<GuardRule> getRules() {
        return rules;
    }

    public void setRules(List<GuardRule> rules) {
        this.rules = rules;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public void setRegistryConfig(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    /**
     * 默认返回配置的第一个数据源配置
     */
    public ResourceConfig getResourceConfig() {
        // 没有配置数据源直接返回空
        if (resources == null) {
            return null;
        }

        for (String resouce : resources.keySet()) {
            return resources.get(resouce);
        }
        return null;
    }

    /**
     * 获取指定Key的数据源配置
     */
    public ResourceConfig getResourceConfig(String resource) {
        return resources.get(resource);
    }

    public Map<String, ResourceConfig> getResources() {
        return resources;
    }

    public void setResources(Map<String, ResourceConfig> resources) {
        this.resources = resources;
    }

    public CacheXConfig getCacheXConfig() {
        return getCacheXConfig(null);
    }

    /**
     * 获取数据源框架配置
     *
     * @param resource 指定的数据源类型，为空则返回第一个数据源配置
     */
    public CacheXConfig getCacheXConfig(String resource) {
        ResourceConfig resourceConfig = null;
        if (resource == null) {
            // 如果不指定数据源则返回第一个数据源配置
            resourceConfig = getResourceConfig();
        } else {
            resourceConfig = getResourceConfig(resource);
        }

        CacheXConfig cacheXConfig = new CacheXConfig();
        if (resourceConfig != null) {
            cacheXConfig.setDialect(resourceConfig.getDialect());
            cacheXConfig.setCache(resourceConfig.getCache());
            CacheXConfig.DbConfig dbConfig = cacheXConfig.getDbConfig();
            DbPoolConfig dbPoolConfig = resourceConfig.getDbPoolConfig();
            if (dbPoolConfig != null) {
                dbConfig.setJdbcUrl(dbPoolConfig.getJdbcUrl());
                dbConfig.setUsername(dbPoolConfig.getUsername());
                dbConfig.setPassword(dbPoolConfig.getPassword());
            }
        }
        return cacheXConfig;
    }

    @Optional("okhttp")
    public static class OkHttpConfig {
        public static final int DEFAULT_RETRY_COUNT = 3;
        public static final int DEFAULT_RETRY_SLEEP_TIME = 200;

        /**
         * 是否采用OkHttp对象注入，供业务方直接使用
         */
        protected boolean enable = false;

        /**
         * 异步轮询器数量
         */
        protected int loopSize = Runtime.getRuntime().availableProcessors();

        /**
         * HTTP请求失败后的重试次数，为0则不重试
         */
        private int retryCount = DEFAULT_RETRY_COUNT;

        /**
         * HTTP请求失败后重试的休眠失败，避免雪崩
         */
        private int retrySleepTime = DEFAULT_RETRY_SLEEP_TIME;

        /**
         * 异步请求服务发现类型
         */
        protected String discoveryType = "QConf";
        protected List<String> discoveryArgs;

        protected Map<String, BalanceMode> balancer;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public int getLoopSize() {
            return loopSize;
        }

        public void setLoopSize(int loopSize) {
            this.loopSize = loopSize;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public int getRetrySleepTime() {
            return retrySleepTime;
        }

        public void setRetrySleepTime(int retrySleepTime) {
            this.retrySleepTime = retrySleepTime;
        }

        public String getDiscoveryType() {
            return discoveryType;
        }

        public void setDiscoveryType(String discoveryType) {
            this.discoveryType = discoveryType;
        }

        public List<String> getDiscoveryArgs() {
            return discoveryArgs;
        }

        public void setDiscoveryArgs(List<String> discoveryArgs) {
            this.discoveryArgs = discoveryArgs;
        }

        public Map<String, BalanceMode> getBalancer() {
            return balancer;
        }

        public void setBalancer(Map<String, BalanceMode> balancer) {
            this.balancer = balancer;
        }
    }

    public static class BalanceMode {
        private String rule;

        private String ping;

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        public String getPing() {
            return ping;
        }

        public void setPing(String ping) {
            this.ping = ping;
        }
    }

    @Optional("registry")
    public static class RegistryConfig {
        /** 是否在启动之后注册服务信息到配置中心，即服务注册 */
        protected boolean enableRegistry = false;

        /** 服务注册的类型，有Zookeeper/QConf/File等 */
        protected String registryType = null;

        /** 配置中心地址 */
        protected String registryUrl = null;

        /** 服务启动时读取的网卡信息，微服务最好统一网卡，例如统一用eth0作为绑定网卡地址 */
        protected String registryInterface = null;

        /** 服役注册Service Id */
        protected String serviceId = null;

        public boolean isEnableRegistry() {
            return enableRegistry;
        }

        public void setEnableRegistry(boolean enableRegistry) {
            this.enableRegistry = enableRegistry;
        }

        public String getRegistryType() {
            return registryType;
        }

        public void setRegistryType(String registryType) {
            this.registryType = registryType;
        }

        public String getRegistryUrl() {
            return registryUrl;
        }

        public void setRegistryUrl(String registryUrl) {
            this.registryUrl = registryUrl;
        }

        public String getRegistryInterface() {
            return registryInterface;
        }

        public void setRegistryInterface(String registryInterface) {
            this.registryInterface = registryInterface;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
    }

    @Optional("rule")
    public static class GuardRule {
        private String type;

        private String resource;

        private int threshold;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }
    }

    @Optional("cachex")
    public static class ResourceConfig {
        /**
         * 数据库方言，默认为MYSQL
         */
        private String dialect;

        /**
         * 缓存类型，默认为JVM内存
         */
        private String cache;

        /**
         * 数据源相关配置
         */
        protected DbPoolConfig dbPoolConfig;

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

        public DbPoolConfig getDbPoolConfig() {
            return dbPoolConfig;
        }

        public void setDbPoolConfig(DbPoolConfig dbPoolConfig) {
            this.dbPoolConfig = dbPoolConfig;
        }
    }

    @Optional("dbpool")
    public static class DbPoolConfig {
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
    }
}
