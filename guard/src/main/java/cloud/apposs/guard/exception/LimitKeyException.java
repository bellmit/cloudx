package cloud.apposs.guard.exception;

/**
 * 限制关键字阻断异常
 */
public class LimitKeyException extends BlockException {
    /**
     * 限制关键字
     */
    private Object limitKey;

    public LimitKeyException(String resource, Object limitKey) {
        super(resource);
        this.limitKey = limitKey;
    }
}
