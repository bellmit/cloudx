package cloud.apposs.rest.parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@link cloud.apposs.rest.annotation.Action}方法参数匹配
 */
public final class Parameter {
	/** 方法参数注解，例如Variable/Body等 */
	private final Annotation annotation;
	
	/** 参数类型 */
	private final Class<?> type;
	
	/** 参数解析器，为了提升性能和简洁，每个参数中只能有一个参数解析器 */
	private ParameterResolver resolver;
	
	/** 参数所在的方法 */
	private final Method method;
	
	/** 参数在方法中位置 */
	private final int index;
	
	public Parameter(Method method, Class<?> type, int index) {
		this(method, type, index, null);
	}

	public Parameter(Method method, Class<?> type, int index, Annotation annotation) {
		this.method = method;
		this.type = type;
		this.index = index;
		this.annotation = annotation;
	}

	public Annotation getAnnotation() {
		return annotation;
	}
	
	public Method getMethod() {
		return method;
	}

	public Class<?> getType() {
		return type;
	}

	public int getIndex() {
		return index;
	}

    @SuppressWarnings("unchecked")
	public <R, P> ParameterResolver<R, P> getResolver() {
		return resolver;
	}

	public void setResolver(ParameterResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder(64);
		info.append("Parameter{");
		info.append("type:").append(type.toString()).append(",");
        info.append("annotation:").append(annotation != null ? true : false);
        info.append("{");
		return info.toString();
	}
}
