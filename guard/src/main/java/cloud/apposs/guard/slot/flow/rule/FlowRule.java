package cloud.apposs.guard.slot.flow.rule;

import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.ControlBehavior;
import cloud.apposs.guard.slot.Rule;
import cloud.apposs.guard.slot.flow.TrafficShapingControl;

/**
 * 流控规则
 */
public class FlowRule extends Rule {
    /**
     * QPS 阈值
     */
    private long threshold;

    /**
     * 限流控制行为
     */
    private ControlBehavior controlBehavior = ControlBehavior.Reject;

    /**
     * 限流整形控制器（判断是否能请求是否能进入资源）
     */
    private TrafficShapingControl control;

    public boolean passCheck(Node node, int token) {
        return control == null ||control.canPass(node, token);
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public TrafficShapingControl getController() {
        return control;
    }

    public void setControl(TrafficShapingControl control) {
        this.control = control;
    }

    public ControlBehavior getControlBehavior() {
        return controlBehavior;
    }

    public void setControlBehavior(ControlBehavior controlBehavior) {
        this.controlBehavior = controlBehavior;
    }
}
