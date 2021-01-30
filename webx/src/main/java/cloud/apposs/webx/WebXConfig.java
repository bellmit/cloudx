package cloud.apposs.webx;

import cloud.apposs.configure.ConfigurationFactory;
import cloud.apposs.configure.ConfigurationParser;
import cloud.apposs.configure.Optional;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 框架全局配置，业务可继承此方法，
 * 在Web.XML配置实现类，实现框架零配置
 */
@Component
public class WebXConfig {
    public static final int DEFAULT_ASYNC_TIMEOUT = 60 * 1000;

    /**
     * 线上模式，即实际生产环境
     */
    public static final int DEV_MODE_ONLINE = 0;
    /**
     * 本地开发模式
     */
    public static final int DEV_MODE_LOCAL = 1;
    /**
     * 预览模式，即部署环境
     */
    public static final int DEV_MODE_DEPLOY = 2;

    /**
     * 扫描基础包，必须配置，框架会自动扫描Action注解类
     */
    protected String basePackage;

    /**
     * 过滤器名称
     */
    protected String filterName;

    /**
     * 是否输出请求日志
     */
    protected boolean httpLogEnable = true;

    /**
     * 请求日志输出格式
     */
    protected String httpLogFormat;

    /**
     * 框架过滤不拦截的URL
     */
    protected String excludeUrlPattern;
    protected String[] excludeUrlPatterns;

    /**
     * 框架过滤不拦截的HTTP REQUEST METHOD
     */
    protected String excludeMethod;
    protected String[] excludeMethods;

    /**
     * 开发模式，不同的开发模式下逻辑或者日志输出级别可能不同，由各业务方定义
     * 0、线上模式，即实际生产环境
     * 2、本地开发模式
     * 3、预览模式，即部署环境
     */
    protected int devMode = DEV_MODE_ONLINE;

    /**
     * Web项目输入输出编码
     */
    protected String charset = "utf-8";

    /**
     * 绑定服务器端口
     */
    private int port = -1;

    /** JSP页面路径配置相关 */
    /**
     * 制定JSP页面存放的路径
     **/
    protected String urlPrefix = "";
    /**
     * 制定JSP文件页面的后缀
     **/
    protected String urlSuffix = ".jsp";

    /**
     * 最大上传文件大小
     */
    protected long maxUploadFileSize = -1;

    /** 网络连接池配置 */
    /**
     * 是否使用网络连接池，不使用则采用每次都是创建网络连接
     */
    protected boolean useClientPooled = false;
    /**
     * 网络连接池连接句柄最小/最大配置
     */
    protected int coreClientPoolSize = 64;
    protected int maxClientPoolSize = Integer.MAX_VALUE;

    /**
     * 是否开启权限注解拦截验证
     */
    protected boolean authDriven = false;

    /**
     * 是否开启请求监控
     */
    protected boolean monitorActive = false;

    /**
     * 当前服务是否为只读，
     * 当开启只读模式时，所有Action中注解WriteCmd的指令均不再响应请求，直接抛出ReadOnlyException，
     * 这种一般只应用于线上服务数据迁移场景，保证用户可以展现页面但无法编辑页面
     */
    private boolean readOnly = false;

    /**
     * 是否输出系统信息
     */
    protected boolean showSysInfo = true;

    /**
     * 异步请求超时时间
     */
    private int asyncTimeout = DEFAULT_ASYNC_TIMEOUT;

    /**
     * 服务发现异步请求组件
     */
    protected OkHttpConfig okHttpConfig;

    /** 日志配置相关 */
    /**
     * 日志输出终端
     */
    protected String logAppender = Appender.CONSOLE;
    /**
     * 日志输出级别，
     * FATAL(致命)、ERROR(错误)、WARN(警告)、INFO(信息)、DEBUG(调试)、OFF(关闭)，
     * 默认为INFO
     */
    protected String logLevel = "INFO";
    /**
     * 日志的存储路径
     */
    protected String logPath = "log";
    /**
     * 日志输出模板
     */
    protected String logFormat = Logger.DEFAULT_LOG_FORMAT;

    /**
     * 限流规则列表
     */
    private List<GuardRule> rules;

    /**
     * 服务注册相关配置
     */
    protected RegistryConfig registryConfig;

    public WebXConfig() {
    }

    public WebXConfig(final InputStream filestream, final int type) throws Exception {
        ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(type);
        cp.parse(this, filestream);
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
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

    public String getExcludeUrlPattern() {
        return excludeUrlPattern;
    }

    public void setExcludeUrlPattern(String excludeUrlPattern) {
        this.excludeUrlPattern = excludeUrlPattern;
    }

    public String[] getExcludeUrlPatterns() {
        return excludeUrlPatterns;
    }

    public void setExcludeUrlPatterns(String[] excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
    }

    public String getExcludeMethod() {
        return excludeMethod;
    }

    public void setExcludeMethod(String excludeMethod) {
        this.excludeMethod = excludeMethod;
    }

    public String[] getExcludeMethods() {
        return excludeMethods;
    }

    public void setExcludeMethods(String[] excludeMethods) {
        this.excludeMethods = excludeMethods;
    }

    public int getDevMode() {
        return devMode;
    }

    public void setDevMode(int devMode) {
        this.devMode = devMode;
    }

    public boolean isDevLocal() {
        return devMode == DEV_MODE_LOCAL;
    }

    public boolean isDevOnline() {
        return devMode == DEV_MODE_ONLINE;
    }

    public boolean isDevDeploy() {
        return devMode == DEV_MODE_DEPLOY;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public String getUrlSuffix() {
        return urlSuffix;
    }

    public void setUrlSuffix(String urlSuffix) {
        this.urlSuffix = urlSuffix;
    }

    public long getMaxUploadFileSize() {
        return maxUploadFileSize;
    }

    public void setMaxUploadFileSize(long maxUploadFileSize) {
        this.maxUploadFileSize = maxUploadFileSize;
    }

    public boolean isUseClientPooled() {
        return useClientPooled;
    }

    public void setUseClientPooled(boolean useClientPooled) {
        this.useClientPooled = useClientPooled;
    }

    public int getCoreClientPoolSize() {
        return coreClientPoolSize;
    }

    public void setCoreClientPoolSize(int coreClientPoolSize) {
        this.coreClientPoolSize = coreClientPoolSize;
    }

    public int getMaxClientPoolSize() {
        return maxClientPoolSize;
    }

    public void setMaxClientPoolSize(int maxClientPoolSize) {
        this.maxClientPoolSize = maxClientPoolSize;
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

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getLogAppender() {
        return logAppender;
    }

    public void setLogAppender(String logAppender) {
        this.logAppender = logAppender;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

    public List<GuardRule> getRules() {
        return rules;
    }

    public void setRules(List<GuardRule> rules) {
        this.rules = rules;
    }

    public OkHttpConfig getOkHttpConfig() {
        return okHttpConfig;
    }

    public void setOkHttpConfig(OkHttpConfig okHttpConfig) {
        this.okHttpConfig = okHttpConfig;
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    public void setRegistryConfig(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    public boolean isShowSysInfo() {
        return showSysInfo;
    }

    public void setShowSysInfo(boolean showSysInfo) {
        this.showSysInfo = showSysInfo;
    }

    public int getAsyncTimeout() {
        return asyncTimeout;
    }

    public void setAsyncTimeout(int asyncTimeout) {
        this.asyncTimeout = asyncTimeout;
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
}
