package cloud.apposs.rest;

import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.SysUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Handler}方法调用辅助类
 */
public final class HandlerInvocation<R, P> {
	public Object invoke(Handler handler, Object target, R request, P response) throws Exception {
		SysUtil.checkNotNull(handler, "handler");
		
		Method beanMethod = handler.getMethod();
		Parameter[] parameters = handler.getParameters();
		List<Object> arguments = new ArrayList<Object>(parameters.length);
		// 解析参数
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			ParameterResolver<R, P> resolver = parameter.getResolver();
			Object argument = resolver.resolveArgument(parameter, request, response);
			arguments.add(argument);
		}
		// 调用方法
		return ReflectUtil.invokeMethod(target, beanMethod, arguments.toArray());
	}
}
