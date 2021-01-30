package cloud.apposs.guard;

import cloud.apposs.guard.slot.ControlBehavior;
import cloud.apposs.guard.slot.flow.rule.FlowRule;
import cloud.apposs.guard.slot.flow.rule.FlowRuleManager;
import cloud.apposs.guard.slot.fuse.rule.FuseRule;
import cloud.apposs.guard.slot.fuse.rule.FuseRule.FuseGrade;
import cloud.apposs.guard.slot.fuse.rule.FuseRuleManager;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRule;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRuleManager;
import cloud.apposs.util.StrUtil;

public class GuardRuleManager {
    public static final String RULE_FLOW = "FLOW";
    public static final String RULE_FUSE = "FUSE";
    public static final String RULE_LIMITKEY = "LIMITKEY";

    public static boolean loadRule(GuardRuleConfig config) {
        String ruleType = config.getType();
        String ruleResource = config.getResource();
        int ruleThreshold = config.getThreshold();
        if (StrUtil.isEmpty(ruleResource) || ruleThreshold <= 0) {
            return false;
        }

        if (RULE_FLOW.equalsIgnoreCase(ruleType)) {
            FlowRule rule = new FlowRule();
            rule.setThreshold(ruleThreshold);
            rule.setResource(ruleResource);
            String controlBehavior = config.getControlBehavior();
            ControlBehavior behavior = ControlBehavior.Reject;
            if (ControlBehavior.WarmUp.name().equalsIgnoreCase(controlBehavior)) {
                behavior = ControlBehavior.WarmUp;
            }
            rule.setControlBehavior(behavior);
            FlowRuleManager.loadRule(rule);
            return true;
        }
        if (RULE_FUSE.equalsIgnoreCase(ruleType)) {
            String fuseGrade = config.getFuseGrade();
            FuseGrade grade = FuseGrade.EXCEPTION_COUNT;
            if (FuseGrade.AVG_RESPTIME.name().equalsIgnoreCase(fuseGrade)) {
                grade = FuseGrade.AVG_RESPTIME;
            } else if(FuseGrade.EXCEPTION_RATE.name().equalsIgnoreCase(fuseGrade)) {
                grade = FuseGrade.EXCEPTION_RATE;
            }
            FuseRule rule = new FuseRule(ruleThreshold, grade);
            rule.setResource(ruleResource);
            rule.setRestoreTimeInMs(config.getRestoreTimeInMs());
            rule.setWarningLine(config.getWarningLine());
            FuseRuleManager.loadRule(rule);
            return true;
        }
        if (RULE_LIMITKEY.equalsIgnoreCase(ruleType)) {
            LimitKeyRule rule = new LimitKeyRule();
            rule.setThreshold(ruleThreshold);
            rule.setResource(ruleResource);
            LimitKeyRuleManager.loadRule(rule);
            return true;
        }

        return false;
    }
}
