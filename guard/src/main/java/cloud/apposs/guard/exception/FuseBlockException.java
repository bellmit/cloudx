package cloud.apposs.guard.exception;

/**
 * 熔断时的阻断异常
 */
public class FuseBlockException extends BlockException {
    public FuseBlockException(String resource) {
        super(resource);
    }
}
