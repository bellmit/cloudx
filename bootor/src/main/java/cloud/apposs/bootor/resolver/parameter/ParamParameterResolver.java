package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.bootor.BootorConstants;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.util.Param;

import java.util.Map;

@Component
public class ParamParameterResolver implements ParameterResolver<HttpRequest, HttpResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return Param.class.isAssignableFrom(parameter.getType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object resolveArgument(Parameter parameter, HttpRequest request, HttpResponse response) throws Exception {
        Param param = new Param();
        param.putAll(request.getParameters());
        param.putAll(request.getParam());
        Map<String, String> uriVariables = (Map) request.getAttribute(BootorConstants.REQUEST_ATTRIBUTE_VARIABLES);
        if (uriVariables != null) {
            param.putAll(uriVariables);
        }
        return param;
    }
}
