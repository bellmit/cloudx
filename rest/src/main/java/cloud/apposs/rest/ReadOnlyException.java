package cloud.apposs.rest;

public class ReadOnlyException extends Exception {
	private static final long serialVersionUID = 3106168042474956392L;

	private final String path;

	public ReadOnlyException(String path) {
	    super("Handler ReadOnly For HTTP Request With URI [" + path + "]");
	    this.path = path;
	}

    public String getPath() {
        return path;
    }
}
