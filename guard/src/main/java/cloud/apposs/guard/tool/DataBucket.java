package cloud.apposs.guard.tool;

/**
 * 数据收集桶，每个限流规则有属于自己的数据收集指标
 */
public interface DataBucket {
    /**
     * 重置数据桶，准备进行下一个收集阶段
     */
    void reset();
}
