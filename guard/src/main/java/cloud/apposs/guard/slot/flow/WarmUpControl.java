package cloud.apposs.guard.slot.flow;

import cloud.apposs.guard.node.Node;

/**
 * 冷启动策略控制器
 */
public class WarmUpControl implements TrafficShapingControl {
    @Override
    public boolean canPass(Node node, int token) {
        return false;
    }
}
