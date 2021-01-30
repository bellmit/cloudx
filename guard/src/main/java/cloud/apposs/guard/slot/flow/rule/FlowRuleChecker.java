package cloud.apposs.guard.slot.flow.rule;

import cloud.apposs.guard.node.Node;

/**
 * 限流规则检查器
 */
public class FlowRuleChecker {
    /**
     * 根据流控规则判断当前指标是否允许进入资源
     */
    public static boolean passCheck(FlowRule rule, Node node, int token) {
        return rule.passCheck(node, token);
    }
}
