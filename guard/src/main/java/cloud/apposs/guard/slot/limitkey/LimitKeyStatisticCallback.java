package cloud.apposs.guard.slot.limitkey;


import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.statistic.StatisticCallback;

/**
 * 限制关键字统计回调
 */
public class LimitKeyStatisticCallback implements StatisticCallback {
    @Override
    public void afterPass(String resource, Node node, ResourceToken resourceToken, int token, Object... args) {
        LimitKeyMetric metric = LimitKeySlot.getMetric(resource);
        if (metric != null && args.length > 0) {
            Object limitKey = args[0];
            metric.addPass(limitKey, token);
        }
    }

    @Override
    public void afterBlocked(BlockException exp, String resource, Node node,
                             ResourceToken resourceToken, int token, Object... args) {
    }
}
