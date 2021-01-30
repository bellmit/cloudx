package cloud.apposs.gateway;

import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.BlockDirective;
import cloud.apposs.gateway.configure.ConfigParser;
import cloud.apposs.util.FileUtil;
import cloud.apposs.util.GetOpt;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * 网关服务入口，
 * 注意：
 * 1、网关是属于业务层和后台层的门面模式，所有请求均是以网关作为第一道出口
 * 2、网关服务除了鉴权、限流、灰度等基本逻辑外，禁止在网关写业务相关的代码，保持网关服务轻量简洁、无状态
 * 启动网关：java -jar gateway-bootstrap.jar -c ~/etc/gateway/gateway-portal.conf
 */
public class GatewayApplication {
    public static void main(String[] args) throws Exception {
        GatewayApplication.run(GatewayApplication.class, args);
    }

    /**
     * 启动运行网关服务
     */
    public static ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        return run(primarySource, generateConfiguration(primarySource, args));
    }

    public static ApplicationContext run(Class<?> primarySource, GatewayConfig config, String... args) throws Exception {
        return new ApplicationContext(config).run(args);
    }

    public static GatewayConfig generateConfiguration(Class<?> primarySource, String... args) throws Exception {
        return generateConfiguration(primarySource, GatewayConstants.DEFAULT_HOST, GatewayConstants.DEFAULT_PORT, args);
    }

    public static GatewayConfig generateConfiguration(Class<?> primarySource, int bindPort, String... args) throws Exception {
        return generateConfiguration(primarySource, GatewayConstants.DEFAULT_HOST, bindPort, args);
    }

    public static GatewayConfig generateConfiguration(Class<?> primarySource, String bindHost, int bindPort, String... args) throws Exception {
        String configFilePath = GatewayConstants.DEFAULT_CONFIG_FILE;
        // 判断是否从命令行中传递配置文件路径
        GetOpt option = new GetOpt(args);
        if (option.containsKey("c")) {
            configFilePath = option.get("c");
        }
        // 加载配置文件配置
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new FileNotFoundException(configFile.getAbsolutePath());
        }
        String content = FileUtil.readString(configFile);
        List<Block> blockList = ConfigParser.parseConfig(content);
        GatewayConfig gatewayConfig = new GatewayConfig();
        // 根据配置文件初始化网关配置
        BlockDirective.initialize(blockList, gatewayConfig);
        return gatewayConfig;
    }

    /**
     * 关闭HTTP服务
     */
    public static void shutdown(ApplicationContext context) {
        if (context != null) {
            context.shutdown();
        }
    }
}
