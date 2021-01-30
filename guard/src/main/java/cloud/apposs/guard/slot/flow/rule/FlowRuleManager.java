package cloud.apposs.guard.slot.flow.rule;

import cloud.apposs.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流规则管理器
 */
public class FlowRuleManager {
    private static Map<String, List<FlowRule>> resourceRulesList = new ConcurrentHashMap<String, List<FlowRule>>();

    /**
     * 加载规则，不支持并发加载和热加载
     */
    public static void loadRule(FlowRule rule) {
        String resource = rule.getResource();
        if (StrUtil.isEmpty(resource)) {
            throw new NullPointerException("resource is null");
        }
        List<FlowRule> rules = resourceRulesList.get(resource);
        if (rules == null) {
            resourceRulesList.put(resource, rules = new ArrayList<FlowRule>());
        }
        FlowRuleSupport.build(rule);
        rules.add(rule);
    }

    /**
     * 根据资源获取规则
     */
    public static List<FlowRule> getRules(String resource) {
        return resourceRulesList.get(resource);
    }
}
