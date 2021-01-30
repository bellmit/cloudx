package cloud.apposs.guard.slot.flow;

import cloud.apposs.guard.node.Node;

/**
 * 流量整形控制器通用接口，实现类根据不同的策略判断当前请求是否能访问资源
 */
public interface TrafficShapingControl {
    boolean canPass(Node node, int token);
}
