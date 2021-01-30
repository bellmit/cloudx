package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServletRequest}参数绑定
 */
@Component
public class RequestParameterResolver implements ParameterResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return ServletRequest.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // HttpServletRequest针对Form表单Post提交的数据如果是普通的表单数据也是能够解析出来的
        return request;
    }
}
