package cloud.apposs.gateway.configure;

public final class Directive {
    /**
     * 日志配置相关指令
     */
    public static final String LOG = "log";
    public static final String LOG_LEVEL = "log_level";
    public static final String LOG_APPENDER = "log_appender";
    public static final String LOG_PATH = "log_path";
    public static final String LOG_FORMAT = "log_format";
    /**
     * 网络配置相关指令
     */
    public static final String NETWORK = "network";
    public static final String NETWORK_NUM_OF_GROUP = "num_of_group";
    public static final String NETWORK_BACKLOG = "backlog";
    public static final String NETWORK_TCP_NODELAY = "tcp_nodelay";
    /**
     * 反向代理配置相关指令
     */
    public static final String UPSTREAM = "upstream";
    public static final String UPSTREAM_SERVER = "server";
    /**
     * 拦截器配置相关指令
     */
    public static final String PREACCESS = "preaccess";
    public static final String GLOBALACCESS = "access";
    public static final String PREACCESS_INTERCEPTOR = "interceptor";
    public static final String PREACCESS_NAME = "interceptor_name";
    public static final String PREACCESS_ARG = "interceptor_arg";
    /**
     * HTTP块配置相关指令
     */
    public static final String HTTP = "http";
    public static final String HTTP_LISTEN = "listen";
    public static final String HTTP_CHARSET = "charset";
    public static final String HTTP_TCP_NODELAY = "tcp_nodelay";
    public static final String HTTP_SHOW_BANNER = "show_banner";
    public static final String HTTP_SERVER = "server";
    public static final String HTTP_SERVER_NAME = "server_name";
    public static final String HTTP_LOATION = "location";
    public static final String HTTP_DEFAULT_TYPE = "default_type";
    public static final String HTTP_ROOT = "root";
    public static final String HTTP_INDEX = "index";
    public static final String HTTP_PROXY_PASS = "proxy_pass";
    public static final String HTTP_SERVICE_PASS = "service_pass";
    public static final String HTTP_SERVICE_REGISTRY = "registry";
    public static final String HTTP_SERVICE_ENVIRONMENT = "environment";
    public static final String HTTP_SERVICE_PATH = "path";
    public static final String HTTP_SITE_PASS = "site_pass";
    public static final String HTTP_INTERCEPTOR_CHAIN = "interceptor_chain";
    public static final String HTTP_ADD_HEADER = "add_header";
    public static final String HTTP_PROXY_SET_HEADER = "proxy_set_header";
    public static final String HTTP_RETURN = "return";
}
