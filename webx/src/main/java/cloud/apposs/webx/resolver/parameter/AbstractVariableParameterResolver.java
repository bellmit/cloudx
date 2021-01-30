package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.VariableParameterResolver;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.webx.WebXConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractVariableParameterResolver extends VariableParameterResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getParameterVariables(Parameter parameter, HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> variables = (Map) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_VARIABLES);
        // 先通过RESTFUL来获取参数，没有则用GET方式来获取参数
        if (variables != null) {
            return variables;
        }
        // 通过Request来获取参数，GET和POST都可用
        Variable variable = (Variable) parameter.getAnnotation();
        String parameterName = variable.value();
        String parameterValue = request.getParameter(parameterName);
        if (parameterValue == null) {
            return null;
        }
        variables = new HashMap<String, String>();
        variables.put(parameterName, parameterValue);
        return variables;
    }
}
