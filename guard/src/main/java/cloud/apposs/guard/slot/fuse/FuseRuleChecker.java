package cloud.apposs.guard.slot.fuse;

import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.fuse.rule.FuseRule;

/**
 * 熔断检查器
 */
public class FuseRuleChecker {
    /**
     * 根据熔断规则判断当前指标是否允许进入资源
     */
    public static boolean passCheck(Node node, FuseRule rule, int token) {
        // 因为涉及到熔断状态的恢复，所以不能通过类方法来做检测，
        // offload 到 FuseRule 的实例方法去做
        return rule.passCheck(node);
    }
}
