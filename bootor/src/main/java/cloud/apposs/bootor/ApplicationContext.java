package cloud.apposs.bootor;

import cloud.apposs.balance.ping.PingFactory;
import cloud.apposs.balance.rule.RuleFactory;
import cloud.apposs.bootor.BootorConfig.GuardRule;
import cloud.apposs.bootor.BootorConfig.RegistryConfig;
import cloud.apposs.bootor.banner.Banner;
import cloud.apposs.bootor.banner.BootorBanner;
import cloud.apposs.bootor.optional.monitor.MonitorAction;
import cloud.apposs.discovery.DiscoveryFactory;
import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.guard.GuardRuleConfig;
import cloud.apposs.guard.GuardRuleManager;
import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.server.http.HttpHandler;
import cloud.apposs.netkit.server.http.HttpServer;
import cloud.apposs.netkit.server.http.HttpSession;
import cloud.apposs.okhttp.HttpBuilder;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.registry.IRegistry;
import cloud.apposs.registry.RegistryFactory;
import cloud.apposs.registry.ServiceInstance;
import cloud.apposs.rest.IGuardProcess;
import cloud.apposs.rest.IHandlerProcess;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.Restful;
import cloud.apposs.util.NetUtil;
import cloud.apposs.util.NetUtil.NetInterface;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SystemInfo;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ApplicationContext {
    /**
     * 全局配置
     */
    private BootorConfig config;

    /**
     * RESTFUL MVC组件
     */
    private Restful<HttpRequest, HttpResponse> restful;
    private IGuardProcess<HttpRequest, HttpResponse> guard;

    private HttpServer bootstrap;

    private ApplicationHandler application;

    /**
     * 异步HTTP请求组件
     */
    private OkHttp okHttp;

    /**
     * 服务注册组件
     */
    private IRegistry registry;

    private Banner.Mode bannerMode = Banner.Mode.CONSOLE;
    private static final Banner DEFAULT_BANNER = new BootorBanner();

    public ApplicationContext() {
        this(new BootorConfig());
    }

    public ApplicationContext(BootorConfig config) {
        this.config = config;
        this.restful = new Restful<HttpRequest, HttpResponse>();
    }

    /**
     * 启动HTTP服务
     */
    @SuppressWarnings("unchecked")
    public ApplicationContext run(Class<?> primarySource, String... args) throws Exception {
        long startTime = System.currentTimeMillis();
        // 初始化日志
        initLogger(config);

        // 注入框架本身的BEAN组件方便业务获取
        BeanFactory beanFactory = restful.getBeanFactory();
        // 将Config配置注入IOC容器中，方便Action直接通过@Autowired来获取Config配置
        beanFactory.addBean(config);
        // 初始化并注入异步OkHttp组件
        okHttp = initOkHttpConfig(config);
        if (okHttp != null) {
            beanFactory.addBean(okHttp);
        }

        // 输出BANNER信息
        Banner banner = beanFactory.getBeanHierarchy(Banner.class);
        if (banner == null) {
            banner = DEFAULT_BANNER;
        }
        initBanner(bannerMode, banner, config.getCharset());
        doPrintSysInfomation();

        // 初始化MVC框架，从框架配置扫描包路径中扫描所有Bean实例
        RestConfig rconfig = initRestConfig(config);
        restful.initialize(rconfig);
        // 初始化框架内部监听服务
        if (config.isMonitorActive()) {
            doActiveMonitor();
        }

        // 初始化熔断参数解析服务
        guard = beanFactory.getBeanHierarchy(IGuardProcess.class);
        initGuardRuleConfig(config);

        // 开始启动HTTP服务
        bootstrap = new HttpServer(config);
        application = new ApplicationHandler();
        bootstrap.setHandler(application).start();

        // 如果开启服务注册的话，注册服务到配置中心，方便客户端进行服务发现和负载均衡
        ServiceInstance serviceInstance = initServiceInstance(config);
        if (serviceInstance != null) {
            RegistryConfig regstConfig = config.getRegistryConfig();
            registry = RegistryFactory.createRegistry(regstConfig.getRegistryType(), regstConfig.getRegistryUrl());
            registry.registInstance(serviceInstance);
        }

        Logger.info("%s Bootor Server Start Success In %d MilliSeconds", primarySource.getSimpleName(),
                (System.currentTimeMillis() - startTime));
        return this;
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

    public void setBannerMode(Banner.Mode bannerMode) {
        this.bannerMode = bannerMode;
    }

    private final class ApplicationHandler extends HttpHandler {
        @Override
        public void service(HttpSession session) throws Exception {
            HttpRequest request = session.getRequest();
            HttpResponse response = session.getResponse();
            // 调用RESTFUL框架进行Handler处理和视图渲染
            restful.renderView(new BootorHandlerProcess(), request, response);
        }

        @Override
        public void destroy() {
            ApplicationContext.this.shutdown();
        }
    }

    /**
     * 初始化日志
     */
    private static void initLogger(BootorConfig config) {
        Properties properties = new Properties();
        properties.put(Configuration.Prefix.APPENDER, config.getLogAppender());
        properties.put(Configuration.Prefix.LEVEL, config.getLogLevel());
        properties.put(Configuration.Prefix.FILE, config.getLogPath());
        properties.put(Configuration.Prefix.FORMAT, config.getLogFormat());
        Logger.config(properties);
    }

    /**
     * 初始化REST配置
     */
    private static RestConfig initRestConfig(BootorConfig config) {
        RestConfig restConfig = new RestConfig();
        restConfig.setCharset(config.getCharset());
        String basePackage = config.getBasePackage();
        // 是否配置中不存在WebX框架中的包，则需要配置进去，方便扫描WebX框架中的各种组件包
        if (!StrUtil.isEmpty(basePackage)) {
            String webxPackage = ReflectUtil.getPackage(ApplicationContext.class) + ".*";
            if (!basePackage.contains(webxPackage)) {
                basePackage += "," + webxPackage;
            }
        }
        restConfig.setBasePackage(basePackage);
        restConfig.setReadOnly(config.isReadOnly());
        restConfig.setAuthDriven(config.isAuthDriven());
        restConfig.setHttpLogEnable(config.isHttpLogEnable());
        restConfig.setHttpLogFormat(config.getHttpLogFormat());
        return restConfig;
    }

    public static OkHttp initOkHttpConfig(BootorConfig config) throws Exception {
        BootorConfig.OkHttpConfig okHttpConfig = config.getOkHttpConfig();
        if (!okHttpConfig.isEnable()) {
            return null;
        }
        // 初始化服务发现组件
        String discoveryType = okHttpConfig.getDiscoveryType();
        List<String> discoveryArgList = okHttpConfig.getDiscoveryArgs();
        String[] discoveryArgs = new String[discoveryArgList.size()];
        discoveryArgList.toArray(discoveryArgs);
        IDiscovery discovery = DiscoveryFactory.createDiscovery(discoveryType, discoveryArgs);
        // 初始化轮询策略和检测策略
        Map<String, BootorConfig.BalanceMode> balancerRules = okHttpConfig.getBalancer();
        for (String serviceId : balancerRules.keySet()) {
            BootorConfig.BalanceMode balanceMode = balancerRules.get(serviceId);
            discovery.setRule(serviceId, RuleFactory.createRule(balanceMode.getRule()));
            discovery.setPing(serviceId, PingFactory.createPing(balanceMode.getPing()));
        }
        // 创建异步HTTP组件并注入到IOC容器中供业务直接使用
        OkHttp okHttp = HttpBuilder.builder()
                .loopSize(okHttpConfig.getLoopSize())
                .retryCount(okHttpConfig.getRetryCount())
                .retrySleepTime(okHttpConfig.getRetrySleepTime())
                .discovery(discovery).build();
        return okHttp;
    }

    /**
     * 初始化熔断配置
     */
    private static void initGuardRuleConfig(BootorConfig config) {
        List<GuardRule> guardRuleList = config.getRules();
        if (guardRuleList == null || guardRuleList.isEmpty()) {
            return;
        }

        for (GuardRule guardRule : guardRuleList) {
            GuardRuleConfig ruleConfig = new GuardRuleConfig();
            ruleConfig.setType(guardRule.getType());
            ruleConfig.setResource(guardRule.getResource());
            ruleConfig.setThreshold(guardRule.getThreshold());
            GuardRuleManager.loadRule(ruleConfig);
        }
    }

    /**
     * 初始化BANNER输出
     */
    private static void initBanner(Banner.Mode bannerMode, Banner banner, String charset) throws Exception {
        if (bannerMode != Banner.Mode.OFF) {
            if (bannerMode == Banner.Mode.CONSOLE) {
                banner.printBanner(System.out);
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                banner.printBanner(new PrintStream(baos));
                Logger.info(baos.toString(charset));
            }
        }
    }

    /**
     * 如果开启服务注册的话，注册服务到配置中心，方便客户端进行服务发现和负载均衡
     */
    private static ServiceInstance initServiceInstance(BootorConfig config) throws Exception {
        RegistryConfig regstConfig = config.getRegistryConfig();
        boolean enabelRegistry = regstConfig.isEnableRegistry() &&
                !StrUtil.isEmpty(regstConfig.getRegistryType()) &&
                !StrUtil.isEmpty(regstConfig.getRegistryInterface()) &&
                !StrUtil.isEmpty(regstConfig.getServiceId());
        if (!enabelRegistry) {
            return null;
        }

        Map<String, NetInterface> interfaces = NetUtil.getLocalAddressInfo();
        if (interfaces.isEmpty()) {
            return null;
        }
        String registryInterface = regstConfig.getRegistryInterface();
        NetInterface netInterface = interfaces.get(registryInterface);
        if (netInterface == null) {
            return null;
        }

        String serviceId = regstConfig.getServiceId();
        ServiceInstance serviceInstance = new ServiceInstance(serviceId,
                netInterface.getLocalAddress().getHostAddress(), config.getPort());
        return serviceInstance;
    }

    /**
     * 启用内部框架监听服务
     */
    private void doActiveMonitor() {
        BeanFactory beanFactory = restful.getBeanFactory();
        // 添加应用程序启动监听
        // 添加Action进行监听逻辑处理
        MonitorAction action = new MonitorAction();
        beanFactory.addBean(action, true);
        restful.addHandler(MonitorAction.class);
    }

    private class BootorHandlerProcess implements IHandlerProcess<HttpRequest, HttpResponse> {
        @Override
        public String getRequestMethod(HttpRequest request, HttpResponse response) {
            return request.getMethod();
        }

        @Override
        public String getRequestPath(HttpRequest request, HttpResponse response) {
            return WebUtil.getRequestPath(request);
        }

        @Override
        public String getRequestHost(HttpRequest request, HttpResponse response) {
            return request.getRemoteHost();
        }

        @Override
        public void processVariable(HttpRequest request, HttpResponse response, Map<String, String> variables) {
            if (variables != null) {
                request.setAttribute(BootorConstants.REQUEST_ATTRIBUTE_VARIABLES, variables);
            }
        }

        @Override
        public IGuardProcess<HttpRequest, HttpResponse> getGuardProcess() {
            return guard;
        }

        @Override
        public void markAsync(HttpRequest request, HttpResponse response) {
            // do nothing
        }
    }

    /**
     * 关闭HTTP服务
     */
    public void shutdown() {
        if (registry != null) {
            registry.release();
        }
        if (okHttp != null) {
            okHttp.close();
        }
        bootstrap.shutdown();
        restful.destroy();
    }
}
