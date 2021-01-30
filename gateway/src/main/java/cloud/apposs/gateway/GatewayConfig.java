package cloud.apposs.gateway;

import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.util.Param;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 网关总配置，网关所有策略均通过此类来调整，支持动态加载
 */
public class GatewayConfig {
    /** 绑定服务器地址 */
    private String host = "0.0.0.0";
    /** 绑定服务器端口 */
    private int port = -1;

    /** 半连接队列数 */
    private int backlog = 1024;

    /**
     * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出去
     * 参考：
     * http://blog.csdn.net/huang_xw/article/details/7340241
     * http://www.open-open.com/lib/view/open1412994697952.html
     */
    private boolean tcpNoDelay = true;

    /**
     * 多少个EventLoop轮询器，主要用于处理各自网络读写数据，
     * 当服务性能不足可提高此配置提升对网络IO的并发处理，但注意EventLoop业务层必须要做到异步，不能有同步阻塞请求
     */
    private int numOfGroup = Runtime.getRuntime().availableProcessors() + 1;

    private String charset = HttpConstants.DEFAULT_CHARSET;

    /** 日志输出终端 */
    private String logAppender = Appender.CONSOLE;

    /**
     * 日志输出级别，
     * FATAL（致命）、
     * ERROR（错误）、
     * WARN（警告）、
     * INFO（信息）、
     * DEBUG（调试）、
     * OFF（关闭），
     * 默认为INFO
     */
    private String logLevel = "INFO";

    /** 日志的存储路径 */
    private String logPath = "logs";

    /** 日志输出模板 */
    private String logFormat = Logger.DEFAULT_LOG_FORMAT;

    /**
     * 以下属性都是通过配置文件来生成的对应对象，供底层网关获取并初始化网关内核
     */
    private final List<Location> locations = new LinkedList<Location>();

    /**
     * 拦截器列表，对应就是拦截器列表名：拦截器列表
     */
    private final Map<String, List<Interceptor>> interceptors = new HashMap<String, List<Interceptor>>();

    /**
     * 反向代理集群列表
     */
    private final Map<String, List<UpstreamServer>> upstreamServers = new HashMap<String, List<UpstreamServer>>();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getNumOfGroup() {
        return numOfGroup;
    }

    public void setNumOfGroup(int numOfGroup) {
        this.numOfGroup = numOfGroup;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
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

    public void addLocation(String path, Location location) {
        locations.add(location);
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void addUpstreamServer(String name, UpstreamServer upstreamServer) {
        List<UpstreamServer> upstreamServerList = upstreamServers.get(name);
        if (upstreamServerList == null) {
            upstreamServerList = new LinkedList<UpstreamServer>();
            upstreamServers.put(name, upstreamServerList);
        }
        upstreamServerList.add(upstreamServer);
    }

    public Map<String, List<UpstreamServer>> getUpstreamServers() {
        return upstreamServers;
    }

    public void addInterceptor(String interceptorGroupName, Interceptor interceptor) {
        List<Interceptor> interceptorList = interceptors.get(interceptorGroupName);
        if (interceptorList == null) {
            interceptorList = new LinkedList<Interceptor>();
            interceptors.put(interceptorGroupName, interceptorList);
        }
        interceptorList.add(interceptor);
    }

    public Map<String, List<Interceptor>> getInterceptors() {
        return interceptors;
    }

    /**
     * Interceptor块，每个个Interceptor块可配置到指定Location块里
     */
    public static class Interceptor {
        /**
         * 拦截器类名
         */
        private final String name;

        /**
         * 拦截器参数列表
         */
        private final Param arguments = new Param();

        public Interceptor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Param getArguments() {
            return arguments;
        }
    }

    /**
     * Location块，一个Location块代表一个后端微服务实例
     */
    public static class Location {
        /**
         * Location块的匹配请求HOST，即server_name *中的*
         */
        private final String host;

        /**
         * Location块的匹配请求URI，即location /中的/
         */
        private final String path;

        private final String contentType;

        /**
         * 对Location块进行数据处理的IHandler类
         */
        private final String clazz;

        /**
         * 拦截器列表
         */
        private final List<Interceptor> interceptorList;

        /**
         * 反向代理自定义输出Header列表
         */
        private final Map<String, String> addHeaders = new HashMap<String, String>();

        /**
         * 反向代理自定义代理请求Header列表
         */
        private final Map<String, String> proxyHeaders = new HashMap<String, String>();

        private final Param options = new Param();

        public Location(String host, String path, String contentType, String clazz, List<Interceptor> interceptorList) {
            this.host = host;
            this.path = path;
            this.contentType = contentType;
            this.clazz = clazz;
            this.interceptorList = interceptorList;
        }

        public String getHost() {
            return host;
        }

        public String getPath() {
            return path;
        }

        public String getContentType() {
            return contentType;
        }

        public String getClazz() {
            return clazz;
        }

        public Param getOptions() {
            return options;
        }

        public List<Interceptor> getInterceptorList() {
            return interceptorList;
        }

        public void addHeader(String key, String value) {
            addHeaders.put(key, value);
        }

        public void addHeaders(Map<String, String> headers) {
            addHeaders.putAll(headers);
        }

        public Map<String, String> addHeaders() {
            return addHeaders;
        }

        public void proxyHeader(String key, String value) {
            proxyHeaders.put(key, value);
        }

        public void proxyHeaders(Map<String, String> headers) {
            proxyHeaders.putAll(headers);
        }

        public Map<String, String> proxyHeaders() {
            return proxyHeaders;
        }
    }

    /**
     * Upstream块，一个Upstream块代表一个微服务集群，
     * 这只是集群配置中的一个，另外微服务集群是通过ZooKeeper来读取，即不通过配置文件配置
     */
    public static class UpstreamServer {
        private final String host;

        private final int port;

        public UpstreamServer(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}
