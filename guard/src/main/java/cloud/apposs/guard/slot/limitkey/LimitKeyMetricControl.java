package cloud.apposs.guard.slot.limitkey;

import cloud.apposs.guard.tool.SlideWindowControl;

import java.util.List;

/**
 * 限制关键字控制器
 */
public class LimitKeyMetricControl extends SlideWindowControl<LimitKeyCounterBucket> {
    public LimitKeyMetricControl(int sampleCount, long intervalInMs) {
        super(sampleCount, intervalInMs);
    }

    @Override
    protected LimitKeyCounterBucket newEmptyBucket() {
        return new LimitKeyCounterBucket();
    }

    public void addKey(Object limitKey, int token) {
        currentWindow().value().add(limitKey, token);
    }

    /**
     * 获取限制关键字的触发总次数
     */
    public long getKeySum(Object limitKey) {
        currentWindow();

        long sum = 0;

        List<LimitKeyCounterBucket> buckets = this.windows();
        for (LimitKeyCounterBucket b : buckets) {
            sum += b.get(limitKey);
        }

        return sum;
    }

    /**
     * 获得限制关键字的 QPS
     */
    public double getKeyQps(Object value) {
        return ((double) getKeySum(value)) / getIntervalInSec();
    }
}
