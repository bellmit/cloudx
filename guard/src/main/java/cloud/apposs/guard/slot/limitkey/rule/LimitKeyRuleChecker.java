package cloud.apposs.guard.slot.limitkey.rule;

import cloud.apposs.guard.slot.limitkey.LimitKeyMetric;

/**
 * 限制关键字检查器
 */
public class LimitKeyRuleChecker {
    /**
     * 根据熔断规则判断当前指标是否允许进入资源
     */
    public static boolean passCheck(LimitKeyMetric metric, LimitKeyRule rule, int token, Object limitKey) {
        // 因为涉及到具体资源计数器，所以不能通过类方法来做检测，
        // offload 到 FuseRule 的实例方法去做
        return rule.passCheck(metric, limitKey, token);
    }
}
