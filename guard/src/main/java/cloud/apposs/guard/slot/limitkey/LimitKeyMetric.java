package cloud.apposs.guard.slot.limitkey;

import cloud.apposs.guard.GuardConstants;

/**
 * 限制关键字数据托管
 */
public class LimitKeyMetric {
    /**
     * 滑动数组统计调用次数
     */
    private LimitKeyMetricControl control = new LimitKeyMetricControl(
            GuardConstants.DEFAULT_WINDOW_SAMPLE_SIZE, GuardConstants.DEFAULT_WINDOW_INTERVAL_IN_MS);

    public double getLimitKeyQps(Object limitKey) {
        return control.getKeyQps(limitKey);
    }

    public void addPass(Object limitKey, int token) {
        if (limitKey == null) {
            return;
        }
        control.addKey(limitKey, token);
    }
}
