package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;

@Component
public class RequestParameterResolver implements ParameterResolver<HttpRequest, HttpResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return HttpRequest.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolveArgument(Parameter parameter, HttpRequest request, HttpResponse response) throws Exception {
        return request;
    }
}
