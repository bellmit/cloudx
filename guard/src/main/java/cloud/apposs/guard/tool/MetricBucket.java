package cloud.apposs.guard.tool;

import java.util.concurrent.atomic.LongAdder;

/**
 * 运行时的指标收集桶
 */
public class MetricBucket implements DataBucket{
    /**
     * 不同的下标，代表不同的域，来收集不同的数据<br/>
     * 具体由 {@link SectionEnum} 指定
     */
    private LongAdder[] sections;

    public MetricBucket() {
        SectionEnum[] sectionValues = SectionEnum.values();
        sections = new LongAdder[sectionValues.length];
        for (int i = 0; i < sectionValues.length; i++) {
            sections[i] = new LongAdder();
        }
    }

    /**
     * 时间窗口重置
     */
    @Override
    public void reset() {
        for (SectionEnum field : SectionEnum.values()) {
            sections[field.ordinal()].reset();
        }
    }

    public void add(SectionEnum field, long count) {
        sections[field.ordinal()].add(count);
    }

    public void addPass(int count) {
        add(SectionEnum.PASS, count);
    }

    public void addBlock (int count) {
        add(SectionEnum.BLOCK, count);
    }

    public void addException (int count) {
        add(SectionEnum.EXCEPTION, count);
    }

    public void addSuccess(int count) {
        add(SectionEnum.SUCCESS, count);
    }

    public void addRespTime(long count) {
        add(SectionEnum.RESPTIME, count);
    }

    public long get(SectionEnum field) {
        return sections[field.ordinal()].sum();
    }

    public enum SectionEnum {
        /**
         * 通过请求数
         */
        PASS,
        /**
         * 阻断请求数
         */
        BLOCK,
        /**
         * 业务异常数
         */
        EXCEPTION,
        /**
         * 成功请求数
         */
        SUCCESS,
        /**
         * 响应时间
         */
        RESPTIME
    }
}
