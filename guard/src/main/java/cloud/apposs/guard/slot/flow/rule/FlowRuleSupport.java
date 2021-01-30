package cloud.apposs.guard.slot.flow.rule;

import cloud.apposs.guard.slot.ControlBehavior;
import cloud.apposs.guard.slot.flow.RejectControl;
import cloud.apposs.guard.slot.flow.TrafficShapingControl;
import cloud.apposs.logger.Logger;

/**
 * 流控规则支持类
 */
public class FlowRuleSupport {
    /**
     * 构建合格的 FlowRule
     */
    public static void build(FlowRule rule) {
        TrafficShapingControl controller = generateControl(rule);
        rule.setControl(controller);
        specRuleCheck(rule);
    }

    /**
     * 特殊规则检查警告
     */
    private static void specRuleCheck(FlowRule rule) {
        TrafficShapingControl controller = rule.getController();
        long threshold = rule.getThreshold();

        // 没有整形控制器
        if (controller == null) {
            Logger.warn("Rule Check;not under control rule");
        }

        // 拦截一切请求
        if (threshold <= 0) {
            Logger.warn("Rule Check;block all request");
        }
    }

    /**
     * 给流控规则生成流量整形控制器
     */
    private static TrafficShapingControl generateControl(FlowRule rule) {
        ControlBehavior behavior = rule.getControlBehavior();
        TrafficShapingControl control = null;
        switch (behavior) {
            case Reject:
                control = new RejectControl(rule.getThreshold());
        }
        return control;
    }
}
