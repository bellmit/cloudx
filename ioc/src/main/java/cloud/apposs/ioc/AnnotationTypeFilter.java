package cloud.apposs.ioc;

import java.lang.annotation.Annotation;

/**
 * 注解包扫描器，匹配扫描包中注解的类
 */
public class AnnotationTypeFilter implements TypeFilter {
	private final Class<? extends Annotation> annotationType;
	
	public AnnotationTypeFilter(Class<? extends Annotation> annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public boolean match(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		if (isMatchAnnotation(clazz)) {
			return true;
		}
		
		// 进行二级注解匹配
		Annotation[] annotations = clazz.getAnnotations();
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> matchAnnotationType = annotation.annotationType();
			Annotation[] subAnnotations = matchAnnotationType.getAnnotations();
			for (Annotation subAnnotation : subAnnotations) {
				Class<? extends Annotation> subAnnotationType = subAnnotation.annotationType();
				if (subAnnotationType.isAssignableFrom(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isMatchAnnotation(Class<?> clazz) {
		return clazz.isAnnotationPresent(annotationType) && !clazz.isInterface() && !clazz.isAnnotation();
	}
}
