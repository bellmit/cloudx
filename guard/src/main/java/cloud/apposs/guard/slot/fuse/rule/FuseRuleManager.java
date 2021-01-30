package cloud.apposs.guard.slot.fuse.rule;

import cloud.apposs.util.StrUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 熔断规则管理器
 */
public class FuseRuleManager {
    private static Map<String, List<FuseRule>> resourceRulesMap = new HashMap<String, List<FuseRule>>();

    /**
     * 加载规则，不支持并发加载和热加载
     */
    public static void loadRule(FuseRule rule) {
        String resource = rule.getResource();
        if (StrUtil.isEmpty(resource)) {
            throw new NullPointerException("resource is null");
        }
        List<FuseRule> rules = resourceRulesMap.get(resource);
        if (rules == null) {
            resourceRulesMap.put(resource, rules = new ArrayList<FuseRule>());
        }
        rules.add(rule);
    }

    /**
     * 根据资源获取规则
     */
    public static List<FuseRule> getRules(String resource) {
        return resourceRulesMap.get(resource);
    }
}
