package cloud.apposs.gateway.interceptor.guard;

import cloud.apposs.gateway.WebUtil;
import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.gateway.interceptor.HandlerInterceptorAdapter;
import cloud.apposs.gateway.variable.VariableParser;
import cloud.apposs.guard.Guard;
import cloud.apposs.guard.GuardRuleConfig;
import cloud.apposs.guard.GuardRuleManager;
import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.Param;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

public class GuardInterceptor extends HandlerInterceptorAdapter {
    /**
     * 熔断Key解析器
     */
    private VariableParser parser;

    /**
     * 熔断资源
     */
    private String resource;

    @Override
    public void init(Param arguments) {
        String resource = arguments.getString("resource");
        if (StrUtil.isEmpty(resource)) {
            throw new IllegalArgumentException("resource");
        }
        String limitKey = arguments.getString("limit_key");
        if (StrUtil.isEmpty(resource)) {
            throw new IllegalArgumentException("limit_key");
        }
        int rate = Parser.parseInt(arguments.getString("rate"), -1);
        if (rate <= 0) {
            throw new IllegalArgumentException("rate");
        }
        String controlBehavior = arguments.getString("control_behavior");
        if (StrUtil.isEmpty(controlBehavior)) {
            throw new IllegalArgumentException("control_behavior");
        }

        // 初始化熔断配置
        GuardRuleConfig ruleConfig = new GuardRuleConfig();
        ruleConfig.setType(GuardRuleManager.RULE_LIMITKEY);
        ruleConfig.setResource(resource);
        ruleConfig.setThreshold(rate);
        ruleConfig.setControlBehavior(controlBehavior);
        GuardRuleManager.loadRule(ruleConfig);
        this.resource = resource;
        this.parser = new VariableParser(limitKey);
    }

    @Override
    public boolean preHandle(HttpRequest request, HttpResponse response, IHandler handler) throws Exception {
        ResourceToken token = null;
        String limitKey = null;
        try {
            limitKey = parser.parse(request, response);
            token = Guard.entry(resource, limitKey);
            return true;
        } catch (BlockException t) {
            Logger.warn("Over Limit Key '%s' Request With URI [%s]", limitKey, WebUtil.getRequestPath(request));
            response.setStatus(HttpStatus.HTTP_STATUS_403);
            response.flush();
            return false;
        } finally {
            if (token != null) {
                token.exit();
            }
        }
    }
}
