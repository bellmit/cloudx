package cloud.apposs.guard.exception;

/**
 * 阻断时抛出的阻断异常
 */
public class BlockException extends Exception {
    private String resource;

    public BlockException(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
