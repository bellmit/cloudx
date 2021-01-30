package cloud.apposs.rest;

public class NoHandlerFoundException extends Exception {
    private static final long serialVersionUID = 3106168042454956372L;

    private final String method;

    private final String path;

    public NoHandlerFoundException(String method, String path) {
        super("No Mapping Handler Found For HTTP '" + method + "' Request With URI [" + path + "]");
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
