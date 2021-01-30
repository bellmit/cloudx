package cloud.apposs.guard.exception;

/**
 * 限流抛出的阻断异常
 */
public class FlowBlockException extends BlockException {
    public FlowBlockException(String resource) {
        super(resource);
    }
}
