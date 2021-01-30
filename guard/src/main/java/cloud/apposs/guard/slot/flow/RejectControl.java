package cloud.apposs.guard.slot.flow;

import cloud.apposs.guard.node.Node;

/**
 * 直接拒绝策略控制器
 */
public class RejectControl implements TrafficShapingControl {
    private long threshold;

    public RejectControl(long threshold ) {
        this.threshold = threshold ;
    }

    @Override
    public boolean canPass(Node node, int token) {
        long qps = node.passQps();
        return qps + token <= threshold;
    }
}
