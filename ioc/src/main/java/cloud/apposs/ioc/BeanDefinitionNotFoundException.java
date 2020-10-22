package cloud.apposs.ioc;

public class BeanDefinitionNotFoundException extends BeansException {
	private static final long serialVersionUID = 3910767531358377481L;
	
	public BeanDefinitionNotFoundException() {
		super();
	}
	
	public BeanDefinitionNotFoundException(String string) {
		super(string);
	}

	public BeanDefinitionNotFoundException(Throwable throwable) {
		super(throwable);
	}
	
	public BeanDefinitionNotFoundException(Class<?> clazz) {
		super(clazz.toString());
	}
	
	public BeanDefinitionNotFoundException(String string, Throwable throwable) {
		super(string, throwable);
	}
}
