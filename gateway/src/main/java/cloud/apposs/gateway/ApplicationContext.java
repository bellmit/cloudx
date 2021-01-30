package cloud.apposs.gateway;

import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.server.http.HttpServer;
import cloud.apposs.netkit.server.http.HttpServerConfig;
import cloud.apposs.util.SystemInfo;

import java.util.List;
import java.util.Properties;

public final class ApplicationContext {
    /**
     * 全局配置
     */
    private GatewayConfig config;

    private HttpServer bootstrap;

    private ApplicationHandler application;

    public ApplicationContext() {
        this(new GatewayConfig());
    }

    public ApplicationContext(GatewayConfig config) {
        this.config = config;
    }

    /**
     * 启动HTTP服务
     */
    public ApplicationContext run(String... args) throws Exception {
        // 初始化日志
        initLogger(config);

        // 输出BANNER信息
        Banner banner = new Banner();
        banner.printBanner(System.out);
        // 输出系统信息
        doPrintSysInfomation();

        // 开始启动HTTP服务
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.setHost(config.getHost());
        httpConfig.setPort(config.getPort());
        httpConfig.setNumOfGroup(config.getNumOfGroup());
        httpConfig.setBacklog(config.getBacklog());
        httpConfig.setTcpNoDelay(config.isTcpNoDelay());
        httpConfig.setLowerHeaderKey(true);
        bootstrap = new HttpServer(httpConfig);
        application = new ApplicationHandler();
        application.initialize(config);
        bootstrap.setHandler(application).start();

        Logger.info("Gateway Application Start Success!");
        return this;
    }

    /**
     * 配置加载新的底层网关处理内核，便于支持服务不重启热加载
     */
    public void setApplication(ApplicationHandler application) {
        this.application = application;
        bootstrap.setHandler(application);
    }

    /**
     * 初始化日志
     */
    private static void initLogger(GatewayConfig config) {
        Properties properties = new Properties();
        properties.put(Configuration.Prefix.APPENDER, config.getLogAppender());
        properties.put(Configuration.Prefix.LEVEL, config.getLogLevel());
        properties.put(Configuration.Prefix.FORMAT, config.getLogFormat());
        properties.put(Configuration.Prefix.FILE, config.getLogPath());
        Logger.config(properties);
    }

    /**
     * 输出系统信息
     */
    private void doPrintSysInfomation() {
        SystemInfo OS = SystemInfo.getInstance();
        Logger.info("OS Name: %s", OS.getOsName());
        Logger.info("OS Arch: %s", OS.getOsArch());
        Logger.info("Java Home: %s", OS.getJavaHome());
        Logger.info("Java Version: %s", OS.getJavaVersion());
        Logger.info("Java Vendor: %s", OS.getJavaVendor());
        List<String> jvmArguments = OS.getJvmArguments();
        for (String argument : jvmArguments) {
            Logger.info("Jvm Argument: [%s]", argument);
        }
    }

    /**
     * 关闭HTTP服务
     */
    public void shutdown() {
        bootstrap.shutdown();
        application.release();
    }
}
