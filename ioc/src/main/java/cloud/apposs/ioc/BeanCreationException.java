package cloud.apposs.ioc;

public class BeanCreationException extends BeansException {
	private static final long serialVersionUID = -7320742324977349304L;

	public BeanCreationException() {
		super();
	}

	public BeanCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public BeanCreationException(String message) {
		super(message);
	}

	public BeanCreationException(Throwable cause) {
		super(cause);
	}
}
