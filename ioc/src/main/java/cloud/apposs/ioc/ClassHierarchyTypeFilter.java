package cloud.apposs.ioc;

import java.lang.annotation.Annotation;

/**
 * 类继承包扫描器，匹配扫描包中有继承指定接口的类
 */
public class ClassHierarchyTypeFilter implements TypeFilter {
	private final Class<?> classHierarchyType;
	
	public ClassHierarchyTypeFilter(Class<? extends Annotation> classHierarchyType) {
		this.classHierarchyType = classHierarchyType;
	}
	
	@Override
	public boolean match(Class<?> clazz) {
		if (clazz == null) {
			return false;
		}
		return classHierarchyType.isAssignableFrom(clazz) && !classHierarchyType.equals(clazz);
	}
}
