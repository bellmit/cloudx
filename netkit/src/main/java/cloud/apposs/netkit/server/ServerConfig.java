package cloud.apposs.netkit.server;

import cloud.apposs.configure.ConfigurationFactory;
import cloud.apposs.configure.ConfigurationParser;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.util.GetOpt;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Properties;

public class ServerConfig {
    public static final long REBIND_SLEEPTIME = 500L;
    public static final int DEFAULT_SEND_TIMEOUT = 60 * 1000;
    public static final int DEFAULT_RECV_TIMEOUT = 60 * 1000;

    private String filename;

    /**
     * 绑定服务器地址
     */
    private String host = "0.0.0.0";
    /**
     * 绑定服务器端口
     */
    private int port = -1;
    /**
     * 绑定的主机列表
     */
    private InetSocketAddress bindSocketAddress;

    private int backlog = 1024;

    /**
     * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出 去
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

    /**
     * 是否开启线程池
     */
    private boolean executorOn = false;
    /**
     * 工作线程池数量
     */
    private int workerCount = Runtime.getRuntime().availableProcessors() << 1;

    /**
     * 服务是否为只读
     */
    private boolean readOnly;

    /**
     * 是否为调式模式
     */
    private boolean debug;

    /**
     * 服务编码
     */
    private String charset = "utf-8";

    /**
     * 发送数据缓存默认分配内存大小
     */
    private int bufferSize = 2 * 1024;
    /**
     * 是否直接使用堆内存
     */
    private boolean bufferDirect = false;

    /**
     * 是否保持服务器端长连接，不检查网络超时
     */
    private boolean keepAlive = false;

    /**
     * 网络接收超时时间
     */
    private int recvTimeout = DEFAULT_RECV_TIMEOUT;
    /**
     * 网络发送超时时间
     */
    private int sendTimeout = DEFAULT_SEND_TIMEOUT;

    /**
     * 日志输出终端
     */
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

    /**
     * 日志的存储路径
     */
    private String logPath = "log";

    /**
     * 日志输出模板
     */
    private String logFormat = Logger.DEFAULT_LOG_FORMAT;

    public ServerConfig() {
    }

    public ServerConfig(String filename) throws Exception {
        this.filename = filename;
        if (filename.toLowerCase().endsWith(".xml")) {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
            cp.parse(this, filename);
        } else {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.JSON);
            cp.parse(this, filename);
        }
    }

    public static ServerConfig parseConfig(String filename) throws Exception {
        ServerConfig config = new ServerConfig(filename);
        return config;
    }

    public static ServerConfig parseConfig(String[] args) throws Exception {
        return parseConfig(args, "c");
    }

    public static ServerConfig parseConfig(String[] args, String option) throws Exception {
        GetOpt getOpt = new GetOpt(args);
        String cfgFile = getOpt.get(option);
        return ServerConfig.parseConfig(cfgFile);
    }

    public static <T> T parseObject(String[] args, T object) throws Exception {
        return parseObject(args, "c", object);
    }

    public static <T> T parseObject(String[] args, String option, T object) throws Exception {
        GetOpt getOpt = new GetOpt(args);
        String cfgFile = getOpt.get(option);
        if (cfgFile.toLowerCase().endsWith(".xml")) {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
            cp.parse(object, cfgFile);
        } else {
            ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.JSON);
            cp.parse(object, cfgFile);
        }
        return object;
    }

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

    public boolean isExecutorOn() {
        return executorOn;
    }

    public void setExecutorOn(boolean executorOn) {
        this.executorOn = executorOn;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isBufferDirect() {
        return bufferDirect;
    }

    public void setBufferDirect(boolean bufferDirect) {
        this.bufferDirect = bufferDirect;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getRecvTimeout() {
        return recvTimeout;
    }

    public void setRecvTimeout(int recvTimeout) {
        this.recvTimeout = recvTimeout;
    }

    public int getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(int sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public InetSocketAddress getBindSocketAddress() {
        if (bindSocketAddress == null) {
            bindSocketAddress = new InetSocketAddress(host, port);
        }
        return bindSocketAddress;
    }

    public void setBindSocketAddress(InetSocketAddress bindSocketAddress) {
        this.bindSocketAddress = bindSocketAddress;
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

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("Config:\n");
        info.append("filename   " + filename + "\n");
        info.append("bindHost   " + host + "\n");
        info.append("bindPort   " + port + "\n");
        info.append("backlog    " + backlog + "\n");
        info.append("numOfGroup " + numOfGroup + "\n");
        info.append("logPath    " + logPath + "\n");
        info.append("logLevel   " + logLevel);
        return info.toString();
    }
}
