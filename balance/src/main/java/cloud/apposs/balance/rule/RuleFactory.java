package cloud.apposs.balance.rule;

import cloud.apposs.balance.IRule;
import cloud.apposs.balance.rule.FairRule;
import cloud.apposs.balance.rule.HashRule;
import cloud.apposs.balance.rule.RandomRule;
import cloud.apposs.balance.rule.RoundRobinRule;
import cloud.apposs.util.StrUtil;

public class RuleFactory {
    /**
     * 基于循环轮询的负载均衡策略
     */
    public static final String RULE_TYPE_ROUNDROBIN = "RoundRobin";
    /**
     * 基于随机轮询的负载均衡策略
     */
    public static final String RULE_TYPE_RAMDOM = "Ramdom";
    /**
     * 基于请求响应时间加权计算的负载均衡策略
     */
    public static final String RULE_TYPE_FAIR = "Fair";
    /**
     * 根据指定的Key进行哈希的负载均衡策略
     */
    public static final String RULE_TYPE_HASH = "Hash";

    public static IRule createRule(String ruleType) {
        if (StrUtil.isEmpty(ruleType)) {
            throw new IllegalArgumentException();
        }
        if (ruleType.equalsIgnoreCase(RULE_TYPE_ROUNDROBIN)) {
            return new RoundRobinRule();
        }
        if (ruleType.equalsIgnoreCase(RULE_TYPE_HASH)) {
            return new HashRule();
        }
        if (ruleType.equalsIgnoreCase(RULE_TYPE_RAMDOM)) {
            return new RandomRule();
        }
        if (ruleType.equalsIgnoreCase(RULE_TYPE_FAIR)) {
            return new FairRule();
        }
        return null;
    }
}
