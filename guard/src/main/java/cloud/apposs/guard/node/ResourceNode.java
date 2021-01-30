package cloud.apposs.guard.node;

import cloud.apposs.guard.GuardConstants;
import cloud.apposs.guard.tool.MetricBucket;
import cloud.apposs.guard.tool.MetricControl;
import cloud.apposs.guard.tool.SlideWindowControl;

/**
 * 资源节点，保存了资源的信息
 */
public class ResourceNode implements Node{
    private String resource;

    /**
     * 滑动数组控制器
     */
    private SlideWindowControl<MetricBucket> control;

    public ResourceNode(String resource) {
        this.control = new MetricControl(GuardConstants.DEFAULT_WINDOW_SAMPLE_SIZE,
                GuardConstants.DEFAULT_WINDOW_INTERVAL_IN_MS);
        this.resource = resource;
    }

    /**
     * 平均响应时间
     */
    @Override
    public long avgRespTime() {
        long rt = getValidWindowData(MetricBucket.SectionEnum.RESPTIME);
        long succ = getValidWindowData(MetricBucket.SectionEnum.SUCCESS);
        if (succ <= 0) {
            return 0;
        }
        return rt / succ;
    }

    /**
     * 正常通过的 QPS 值
     */
    @Override
    public long passQps() {
        return getValidWindowData(MetricBucket.SectionEnum.PASS) / control.getIntervalInSec();
    }

    /**
     * 阻断的 QPS
     */
    @Override
    public long blockQps() {
        return getValidWindowData(MetricBucket.SectionEnum.BLOCK) / control.getIntervalInSec();
    }

    /**
     * 异常的 QPS
     */
    @Override
    public long exceptionQps() {
        return getValidWindowData(MetricBucket.SectionEnum.EXCEPTION) / control.getIntervalInSec();
    }

    @Override
    public long successQps() {
        return getValidWindowData(MetricBucket.SectionEnum.SUCCESS) / control.getIntervalInSec();
    }

    @Override
    public long totalQps() {
        return passQps() + blockQps();
    }

    @Override
    public void addException(int count) {
        control.currentWindow().value().addException(count);
    }

    @Override
    public void addBlock(int count) {
        control.currentWindow().value().addBlock(count);
    }

    @Override
    public void addPass(int count) {
        control.currentWindow().value().addPass(count);
    }

    @Override
    public void addRespTimeAndSuccCount(long respTime, int count) {
        control.currentWindow().value().addRespTime(respTime);
        control.currentWindow().value().addSuccess(count);
    }

    public String getResource() {
        return resource;
    }

    /**
     * 根据数据域获取有效窗口的统计值
     */
    private long getValidWindowData(MetricBucket.SectionEnum type) {
        long data = 0;
        for (MetricBucket window : control.windows()) {
            data += window.get(type);
        }
        return data;
    }
}
