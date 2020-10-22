package cloud.apposs.netkit.server.annotation;

import java.util.HashMap;
import java.util.Map;

public class AnnotationParserSupport {
	private final Map<Class<?>, AnnotationParser> parses;
	
	public AnnotationParserSupport() {
		this.parses = new HashMap<Class<?>, AnnotationParser>(32);
	}
	
	/**
	 * 获取注解解析器
	 */
	public AnnotationParser getAnnotationParser(Class<?> requiredType) {
		return parses.get(requiredType);
	}
	
	/**
	 * 添加注解解析器
	 */
	public AnnotationParser addPropertyEditor(Class<?> type, AnnotationParser parser) {
		return parses.put(type, parser);
	}
	
	/**
	 * 批量添加注解解析器
	 */
	public void addAllPropertyEditor(Map<Class<?>, AnnotationParser> parses) {
		this.parses.putAll(parses);
	}
}
