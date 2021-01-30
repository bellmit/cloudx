package cloud.apposs.rest;

public class RestException extends RuntimeException {
    private static final long serialVersionUID = 1786561340789816150L;

    public RestException() {
        super();
    }

    public RestException(String message) {
        super(message);
    }

    public RestException(Throwable cause) {
        super(cause);
    }

    public RestException(String message, Throwable cause) {
        super(message, cause);
    }
}
