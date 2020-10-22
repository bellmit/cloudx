package cloud.apposs.ioc;

public class BeanDefinitionStoreException extends BeansException {
	private static final long serialVersionUID = 4046957487631872595L;

	public BeanDefinitionStoreException() {
		super();
	}

	public BeanDefinitionStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public BeanDefinitionStoreException(String message) {
		super(message);
	}

	public BeanDefinitionStoreException(Throwable cause) {
		super(cause);
	}
}
