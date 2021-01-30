package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.util.Param;
import cloud.apposs.webx.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * {@link Param}参数绑定
 */
@Component
public class ParamParameterResolver implements ParameterResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return Param.class.isAssignableFrom(parameter.getType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object resolveArgument(Parameter parameter,
                HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebXConfig config = (WebXConfig) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_WEBXCONFIG);
        Param param = WebUtil.getRequestParam(request, config.getCharset());
        Map<String, String> uriVariables = (Map) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_VARIABLES);
        if (uriVariables != null) {
            param.putAll(uriVariables);
        }
        return param;
    }
}
