package cloud.apposs.rest.parameter;

/**
 * 方法参数解析器
 */
public interface ParameterResolver<R, P> {
	/** 指定的参数类型是否支持 */
	boolean supportsParameter(Parameter parameter);
	
	/** 解析参数类型 */
	Object resolveArgument(Parameter parameter, R request, P response) throws Exception;
}
