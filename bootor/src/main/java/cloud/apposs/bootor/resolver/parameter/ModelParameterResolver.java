package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.bootor.BootorConstants;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.parameter.BodyParameterResolver;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.util.Param;

import java.util.Map;

@Component
public class ModelParameterResolver extends BodyParameterResolver<HttpRequest, HttpResponse> {
    @Override
    @SuppressWarnings("unchecked")
    public Param getParameterValues(Parameter parameter, HttpRequest request, HttpResponse response) {
        Param param = new Param();
        param.putAll(request.getParameters());
        param.putAll(request.getParam());

        // 把请求流水号打进Model对象，方便在进行HTTP请求时也把流水号带上
        Object flowValue = request.getAttribute(BootorConstants.REQUEST_PARAMETRIC_FLOW);
        if (flowValue instanceof Long) {
            long flow = (long) flowValue;
            param.put(BootorConstants.REQUEST_PARAMETRIC_FLOW, flow);
        }
        // 把HttpRequest和HttpResponse也打进Mddel对象中
        param.put(BootorConstants.REQUEST_PARAMETRIC_REQUEST, request);
        param.put(BootorConstants.REQUEST_PARAMETRIC_RESPONSE, response);

        Map<String, String> uriVariables = (Map) request.getAttribute(BootorConstants.REQUEST_ATTRIBUTE_VARIABLES);
        if (uriVariables != null) {
            param.putAll(uriVariables);
        }
        return param;
    }
}
