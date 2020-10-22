package cloud.apposs.ioc;

/**
 * 包扫描过滤器
 */
public interface TypeFilter {
	boolean match(Class<?> clazz);
}
