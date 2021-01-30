package cloud.apposs.bootor;

public class BootorException extends RuntimeException {
    private static final long serialVersionUID = 1786561340789816140L;

    public BootorException() {
        super();
    }

    public BootorException(String message) {
        super(message);
    }

    public BootorException(Throwable cause) {
        super(cause);
    }

    public BootorException(String message, Throwable cause) {
        super(message, cause);
    }
}
