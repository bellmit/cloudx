package cloud.apposs.guard.slot;

/**
 * 限流控制行为
 */
public enum ControlBehavior {
    /**
     * 直接拒绝
     */
    Reject,

    /**
     * 冷启动
     */
    WarmUp
}
