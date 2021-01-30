package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.GatewayConstants;
import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.Directive;
import cloud.apposs.gateway.handler.index.IndexHandler;
import cloud.apposs.gateway.handler.index.ReturnHandler;
import cloud.apposs.gateway.handler.proxy.ProxyHandler;
import cloud.apposs.gateway.handler.proxy.ServiceHandler;
import cloud.apposs.gateway.handler.proxy.SiteHandler;
import cloud.apposs.util.Parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HttpDirective extends AbstractDirective {
    @Override
    public void parse(Block block, GatewayConfig config) throws ConfigParseException {
        checkBlockArgumentMustEmpty(block);
        List<Block> values = block.getValues();
        for (Block value : values) {
            String directive = value.getKey();
            switch (directive) {
                case Directive.HTTP_LISTEN:
                    List<String> arguments = getNonBlockArgumentTwo(value);
                    String host = arguments.get(0);
                    int port = Parser.parseInt(arguments.get(1));
                    config.setHost(host);
                    config.setPort(port);
                    break;
                case Directive.HTTP_CHARSET:
                    String charset = getNonBlockArgumentOne(value);
                    config.setCharset(charset);
                    break;
                case Directive.HTTP_TCP_NODELAY:
                    boolean isTcpNodelay = Parser.parseBoolean(getNonBlockArgumentOne(value));
                    config.setTcpNoDelay(isTcpNodelay);
                    break;
                case Directive.HTTP_SHOW_BANNER:
                    boolean isShowBanner = Parser.parseBoolean(getNonBlockArgumentOne(value));
                    config.setTcpNoDelay(isShowBanner);
                    break;
                case Directive.HTTP_SERVER:
                    doParserServerBlock(value.getValues(), config);
                    break;
                default:
                    throw new ConfigParseException(value.getLineNo(), "unknown directive \"" + directive + "\"");
            }
        }
    }

    private void doParserServerBlock(List<Block> blockList, GatewayConfig config) throws ConfigParseException {
        String serverName = doGetServerName(blockList);
        for (Block block : blockList) {
            String directive = block.getKey();
            switch (directive) {
                case Directive.HTTP_SERVER_NAME:
                    break;
                case Directive.HTTP_LOATION:
                    String locationPath = getBlockArgumentOne(block);
                    List<Block> locationValues = block.getValues();
                    doParserLocationBlock(serverName, locationPath, locationValues, config);
                    break;
                default:
                    throw new ConfigParseException(block.getLineNo(), "unknown directive \"" + directive + "\"");
            }
        }
    }

    private String doGetServerName(List<Block> blockList) throws ConfigParseException {
        String serverName = GatewayConstants.GATEWAY_CONF_UNIVERSAL_MATCH;
        for (Block block : blockList) {
            String directive = block.getKey();
            if (directive.equals(Directive.HTTP_SERVER_NAME)) {
                serverName = getNonBlockArgumentOne(block);
                break;
            }
        }
        return serverName;
    }

    private void doParserLocationBlock(String serverName, String locationPath,
            List<Block> blockList, GatewayConfig config) throws ConfigParseException {
        // 将参数按directive:block做映射，方便快速查找匹配
        Map<String, Block> locationValueMap = new HashMap<String, Block>();
        for (Block location : blockList) {
            String locationDirective = location.getKey();
            locationValueMap.put(locationDirective, location);
        }
        // 检查配置参数的合法性
        List<String> validLocationDirectiveList = new LinkedList<String>();
        validLocationDirectiveList.add(Directive.HTTP_DEFAULT_TYPE);
        validLocationDirectiveList.add(Directive.HTTP_ROOT);
        validLocationDirectiveList.add(Directive.HTTP_INDEX);
        validLocationDirectiveList.add(Directive.HTTP_PROXY_PASS);
        validLocationDirectiveList.add(Directive.HTTP_SERVICE_PASS);
        validLocationDirectiveList.add(Directive.HTTP_SERVICE_REGISTRY);
        validLocationDirectiveList.add(Directive.HTTP_SERVICE_ENVIRONMENT);
        validLocationDirectiveList.add(Directive.HTTP_SERVICE_PATH);
        validLocationDirectiveList.add(Directive.HTTP_SITE_PASS);
        validLocationDirectiveList.add(Directive.HTTP_INTERCEPTOR_CHAIN);
        validLocationDirectiveList.add(Directive.HTTP_ADD_HEADER);
        validLocationDirectiveList.add(Directive.HTTP_PROXY_SET_HEADER);
        validLocationDirectiveList.add(Directive.HTTP_RETURN);
        for (String locationDirective : locationValueMap.keySet()) {
            Block locationValue = locationValueMap.get(locationDirective);
            if (!validLocationDirectiveList.contains(locationDirective)) {
                throw new ConfigParseException(locationValue.getLineNo(), "unknown directive \"" + locationDirective + "\"");
            }
        }
        // 解析通用的default_type指令
        String contentType = GatewayConstants.DEFAULT_CONTENT_TYPE;
        if (locationValueMap.containsKey(Directive.HTTP_DEFAULT_TYPE)) {
            contentType = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_DEFAULT_TYPE));
        }
        // 解析通用的interceptor_chain指令
        List<GatewayConfig.Interceptor> interceptorList = new LinkedList<GatewayConfig.Interceptor>();
        Map<String, List<GatewayConfig.Interceptor>> interceptors = config.getInterceptors();
        if (locationValueMap.containsKey(Directive.HTTP_INTERCEPTOR_CHAIN)) {
            Block locationValue = locationValueMap.get(Directive.HTTP_INTERCEPTOR_CHAIN);
            String interceptorChainName = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_INTERCEPTOR_CHAIN));
            if (!interceptors.containsKey(interceptorChainName)) {
                throw new ConfigParseException(locationValue.getLineNo(), "inerceptor not found in preaccess \"" + interceptorChainName + "\"");
            }
            List<GatewayConfig.Interceptor> globalInterceptors = interceptors.get(GlobalAccessDirective.GLOBAL_ACCESS_NAME);
            interceptorList = interceptors.get(interceptorChainName);
        }
        List<GatewayConfig.Interceptor> globalInterceptors = interceptors.get(GlobalAccessDirective.GLOBAL_ACCESS_NAME);
        if (globalInterceptors != null) {
            interceptorList.addAll(globalInterceptors);
        }
        // 解析通用的add_header指令
        Map<String, String> addHeaders = new HashMap<String, String>();
        if (locationValueMap.containsKey(Directive.HTTP_ADD_HEADER)) {
            for (Block location : blockList) {
                if (!location.getKey().equals(Directive.HTTP_ADD_HEADER)) {
                    continue;
                }
                List<String> locationArguments = location.getArguments();
                if (locationArguments.size() < 2) {
                    throw new ConfigParseException(location.getLineNo(),
                            "nvalid number of arguments in \"" + Directive.HTTP_ADD_HEADER + "\" directive");
                }
                String headerValue = getNonBlockArgumentListStringTwo(location).trim();
                addHeaders.put(locationArguments.get(0), headerValue);
            }
        }
        // 解析通用的proxy_header指令
        Map<String, String> proxyHeaders = new HashMap<String, String>();
        if (locationValueMap.containsKey(Directive.HTTP_PROXY_SET_HEADER)) {
            for (Block location : blockList) {
                if (!location.getKey().equals(Directive.HTTP_PROXY_SET_HEADER)) {
                    continue;
                }
                List<String> locationArguments = location.getArguments();
                if (locationArguments.size() < 2) {
                    throw new ConfigParseException(location.getLineNo(),
                            "nvalid number of arguments in \"" + Directive.HTTP_PROXY_SET_HEADER + "\" directive");
                }
                String headerValue = getNonBlockArgumentListStringTwo(location).trim();
                if (headerValue.startsWith("'") || headerValue.startsWith("\"")) {
                    headerValue = headerValue.substring(1);
                }
                if (headerValue.endsWith("'") || headerValue.endsWith("\"")) {
                    headerValue = headerValue.substring(0, headerValue.length() - 1);
                }
                addHeaders.put(locationArguments.get(0), headerValue);
            }
        }
        // 进行对应的指令解析
        for (String locationDirective : locationValueMap.keySet()) {
            Block locationValue = locationValueMap.get(locationDirective);
            // 解析root指令，即IndexHandler
            if (locationDirective.equals(Directive.HTTP_ROOT)) {
                String directory = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_ROOT));
                String index = locationValueMap.containsKey(Directive.HTTP_INDEX) ?
                        getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_INDEX)) : "index.html";
                GatewayConfig.Location locationCfg = new GatewayConfig.Location(serverName,
                        locationPath, contentType, IndexHandler.class.getName(), interceptorList);
                locationCfg.getOptions().put("directory", directory);
                locationCfg.getOptions().put("index", index);
                locationCfg.addHeaders(addHeaders);
                locationCfg.proxyHeaders(proxyHeaders);
                config.addLocation(locationPath, locationCfg);
            }
            // 解析proxy_pass指令，即ProxyHandler
            if (locationDirective.equals(Directive.HTTP_PROXY_PASS)) {
                String proxyKey = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_PROXY_PASS));
                int proxyPassProtocolIdx = -1;
                if (proxyKey.indexOf("http://") != -1) {
                    proxyPassProtocolIdx = 7;
                } else if (proxyKey.indexOf("https://") != -1) {
                    proxyPassProtocolIdx = 8;
                }
                if (proxyPassProtocolIdx == -1) {
                    throw new ConfigParseException(locationValue.getLineNo(), "invalid URL prefix");
                }
                String upstreamKey = proxyKey.substring(proxyPassProtocolIdx);
                Map<String, List<GatewayConfig.UpstreamServer>> upstreamServers = config.getUpstreamServers();
                if (!upstreamServers.containsKey(upstreamKey)) {
                    throw new ConfigParseException(locationValue.getLineNo(), "host not found in upstream \"" + upstreamKey + "\"");
                }
                List<GatewayConfig.UpstreamServer> upstreamServerList = upstreamServers.get(upstreamKey);
                GatewayConfig.Location locationCfg = new GatewayConfig.Location(serverName,
                        locationPath, contentType, ProxyHandler.class.getName(), interceptorList);
                locationCfg.getOptions().put("key", upstreamKey);
                locationCfg.getOptions().put("upstream", upstreamServerList);
                locationCfg.addHeaders(addHeaders);
                locationCfg.proxyHeaders(proxyHeaders);
                config.addLocation(locationPath, locationCfg);
            }
            // 解析proxy_pass指令，即SiteHandler
            if (locationDirective.equals(Directive.HTTP_SITE_PASS)) {
                String proxyUrl = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_SITE_PASS));
                if (!proxyUrl.startsWith("http://") && !proxyUrl.startsWith("https://")) {
                    proxyUrl = "http://" + proxyUrl;
                }
                GatewayConfig.Location locationCfg = new GatewayConfig.Location(serverName,
                        locationPath, contentType, SiteHandler.class.getName(), interceptorList);
                locationCfg.getOptions().put("proxyUrl", proxyUrl);
                locationCfg.addHeaders(addHeaders);
                locationCfg.proxyHeaders(proxyHeaders);
                config.addLocation(locationPath, locationCfg);
            }
            // 解析service_pass指令，即ServiceHandler
            if (locationDirective.equals(Directive.HTTP_SERVICE_PASS)) {
                String serviceId = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_SERVICE_PASS));
                String registry = locationValueMap.containsKey(Directive.HTTP_SERVICE_REGISTRY) ?
                        getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_SERVICE_REGISTRY)) : "qconf";
                if (!locationValueMap.containsKey(Directive.HTTP_SERVICE_ENVIRONMENT)) {
                    throw new ConfigParseException(locationValue.getLineNo(), "require directive \"environment\"");
                }
                if (!locationValueMap.containsKey(Directive.HTTP_SERVICE_PATH)) {
                    throw new ConfigParseException(locationValue.getLineNo(), "require directive \"path\"");
                }
                String environment = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_SERVICE_ENVIRONMENT));
                String path = getNonBlockArgumentOne(locationValueMap.get(Directive.HTTP_SERVICE_PATH));
                GatewayConfig.Location locationCfg = new GatewayConfig.Location(serverName,
                        locationPath, contentType, ServiceHandler.class.getName(), interceptorList);
                locationCfg.getOptions().put("serviceId", serviceId);
                locationCfg.getOptions().put("registry", registry);
                locationCfg.getOptions().put("environment", environment);
                locationCfg.getOptions().put("path", path);
                locationCfg.addHeaders(addHeaders);
                locationCfg.proxyHeaders(proxyHeaders);
                config.addLocation(locationPath, locationCfg);
            }
            // 解析return指令，即ReturnHandler
            if (locationDirective.equals(Directive.HTTP_RETURN)) {
                List<String> arguments = getNonBlockArgumentListTwo(locationValueMap.get(Directive.HTTP_RETURN));
                GatewayConfig.Location locationCfg = new GatewayConfig.Location(serverName,
                        locationPath, contentType, ReturnHandler.class.getName(), interceptorList);
                locationCfg.getOptions().put("status", arguments.get(0));
                locationCfg.getOptions().put("content", arguments.get(1));
                locationCfg.addHeaders(addHeaders);
                locationCfg.proxyHeaders(proxyHeaders);
                config.addLocation(locationPath, locationCfg);
            }
        }

    }
}
