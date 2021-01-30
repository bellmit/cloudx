package cloud.apposs.websocket;

import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.netkit.server.http.HttpServerConfig;
import cloud.apposs.netkit.server.websocket.WebSocketHandler;
import cloud.apposs.netkit.server.websocket.WebSocketServer;
import cloud.apposs.util.SystemInfo;

import java.util.List;
import java.util.Properties;

public final class ApplicationContext {
    /**
     * 全局配置
     */
    private WSConfig config;

    private WebSocketServer bootstrap;

    private ApplicationHandler application;

    public ApplicationContext() {
        this(new WSConfig());
    }

    public ApplicationContext(WSConfig config) {
        this.config = config;
    }

    /**
     * 启动WebSocket服务
     */
    public ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        // 初始化日志
        initLogger(config);
        // 输出BANNER信息
        Banner banner = new Banner();
        banner.printBanner(System.out);
        doPrintSysInfomation();
        // 开始启动WebSocket服务
        bootstrap = new WebSocketServer(config);
        // 初始化组件
        application = new ApplicationHandler();
        application.initialize(config);
        // 开始启动服务
        bootstrap.setHandler(application).start();
        Logger.info("%s WebSocket Server Start Success In %d MilliSeconds", primarySource.getSimpleName(),
                (System.currentTimeMillis() - startTime));
        return this;
    }

    /**
     * 初始化日志
     */
    private static void initLogger(WSConfig config) {
        Properties properties = new Properties();
        properties.put(Configuration.Prefix.APPENDER, config.getLogAppender());
        properties.put(Configuration.Prefix.LEVEL, config.getLogLevel());
        properties.put(Configuration.Prefix.FILE, config.getLogPath());
        properties.put(Configuration.Prefix.FORMAT, config.getLogFormat());
        Logger.config(properties);
    }

    /**
     * 输出系统信息
     */
    private void doPrintSysInfomation() {
        if (config.isShowSysInfo()) {
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
    }

    public void shutdown() {
        bootstrap.shutdown();
    }
}
