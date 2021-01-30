package cloud.apposs.rest.parameter;

import cloud.apposs.util.SysUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 请求参数解析管理器
 */
public final class ParameterResolverSupport {
	/** 参数解析器 */
    private final List<ParameterResolver> parameterResolvers = new LinkedList<ParameterResolver>();
    
    public ParameterResolver getParameterResolver(Parameter parameter) {
    	for (ParameterResolver parameterResolver : parameterResolvers) {
			if (parameterResolver.supportsParameter(parameter)) {
				return parameterResolver;
			}
		}
		return null;
	}
    
    public List<ParameterResolver> getParameterResolverList(Parameter parameter) {
    	List<ParameterResolver> parameterResolverList = new LinkedList<ParameterResolver>();
    	for (ParameterResolver parameterResolver : parameterResolvers) {
			if (parameterResolver.supportsParameter(parameter)) {
				parameterResolverList.add(parameterResolver);
			}
		}
		return parameterResolverList;
	}
    
    public void addParameterResolver(ParameterResolver resolver) {
    	SysUtil.checkNotNull(resolver, "resolver");
		parameterResolvers.add(resolver);
	}

	public int getParameterResolverSize() {
		return parameterResolvers.size();
	}
}
