package cloud.apposs.guard.slot.limitkey.rule;

import cloud.apposs.guard.slot.Rule;
import cloud.apposs.guard.slot.limitkey.LimitKeyMetric;

/**
 * 限制关键字流控规则
 * 支持资源 QPS 限流搭载关键字 QPS 热点限流
 */
public class LimitKeyRule extends Rule {
    /**
     * 限制关键字 QPS 阈值
     */
    private long threshold;

    public boolean passCheck(LimitKeyMetric metric, Object limitKey, int token) {
        double curQps = metric.getLimitKeyQps(limitKey);
        return curQps + token <= threshold;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }
}
