package cloud.apposs.rest.parameter;

import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Variable}=注解参数绑定
 */
public abstract class VariableParameterResolver<R, P> implements ParameterResolver<R, P> {
	@Override
	public boolean supportsParameter(Parameter parameter) {
		Annotation annotation = parameter.getAnnotation();
		if (annotation == null || !Variable.class.isAssignableFrom(annotation.annotationType())) {
			return false;
		}
		String parameterName = ((Variable) annotation).value();
		return !StrUtil.isEmpty(parameterName) && isParameterTypeSupports(parameter.getType());
	}

	@Override
	public Object resolveArgument(Parameter parameter, R request, P response) throws Exception {
		Map<String, String> variables = getParameterVariables(parameter, request, response);
		Variable variable = (Variable) parameter.getAnnotation();
        String parameterValue = null;
		if (variables != null) {
            String parameterName = variable.value();
            parameterValue = variables.get(parameterName);
        }
		
		// 前端传递参数为空则看是否配置了默认值
		if (StrUtil.isEmpty(parameterValue)) {
			parameterValue = variable.defaultValue();
		}
		
		// 参数为必选但参数传递为空，抛出异常
		if (StrUtil.isEmpty(parameterValue) && variable.required()) {
			throw new RequestParameterMissingException(parameter);
		}
		
		return castParameterValue(parameterValue);
	}

	public abstract Map<String, String> getParameterVariables(Parameter parameter, R request, P response);
	
	/**
	 * 参数类型是否匹配
	 */
	public abstract boolean isParameterTypeSupports(Class<?> parameterType);
	
	/**
	 * 参数类型转换
	 */
	public abstract Object castParameterValue(String parameterValue);
}
