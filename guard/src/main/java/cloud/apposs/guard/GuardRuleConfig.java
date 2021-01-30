package cloud.apposs.guard;

import cloud.apposs.guard.slot.fuse.rule.FuseRule;

public class GuardRuleConfig {
    /** 规则类型 */
    private String type;

    /** 资源名称 */
    private String resource;

    /** 限流阀值 */
    private int threshold;

    /**
     * 限流策略
     */
    private String controlBehavior;

    /**
     * 降级规则是什么，有异常数据降级，也有响应时间降级
     */
    private String fuseGrade;
    /**
     * 警告线，不同的熔断策略有不同的意义
     */
    private int warningLine = GuardConstants.DEFAULT_WARNING_LINE;
    /**
     * 熔断恢复时间（ms）
     */
    private long restoreTimeInMs = GuardConstants.DEFAULT_RESTORE_TIME;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getControlBehavior() {
        return controlBehavior;
    }

    public void setControlBehavior(String controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    public String getFuseGrade() {
        return fuseGrade;
    }

    public void setFuseGrade(String fuseGrade) {
        this.fuseGrade = fuseGrade;
    }

    public int getWarningLine() {
        return warningLine;
    }

    public void setWarningLine(int warningLine) {
        this.warningLine = warningLine;
    }

    public long getRestoreTimeInMs() {
        return restoreTimeInMs;
    }

    public void setRestoreTimeInMs(long restoreTimeInMs) {
        this.restoreTimeInMs = restoreTimeInMs;
    }
}
