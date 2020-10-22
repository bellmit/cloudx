package cloud.apposs.ioc;

public class BeansException extends RuntimeException {
	private static final long serialVersionUID = 4046957487631872595L;

	public BeansException() {
		super();
	}

	public BeansException(String message, Throwable cause) {
		super(message, cause);
	}

	public BeansException(String message) {
		super(message);
	}

	public BeansException(Throwable cause) {
		super(cause);
	}
}
