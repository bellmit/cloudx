package cloud.apposs.guard.tool;

/**
 * 通用数据控制器
 */
public class MetricControl extends SlideWindowControl<MetricBucket> {
    public MetricControl(int sampleCount, long intervalInMs) {
        super(sampleCount, intervalInMs);
    }

    @Override
    protected MetricBucket newEmptyBucket() {
        return new MetricBucket();
    }
}
