package cloud.apposs.guard.slot.statistic;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;

/**
 * 统计槽回调接口
 */
public interface StatisticCallback {
    /**
     * pass 后回调
     */
    public void afterPass(String resource, Node node, ResourceToken resourceToken, int token, Object... args);

    /**
     * blocked 后回调
     */
    public void afterBlocked(BlockException exp, String resource, Node node, ResourceToken resourceToken, int token, Object... args);
}
