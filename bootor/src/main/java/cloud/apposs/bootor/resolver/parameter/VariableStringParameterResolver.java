package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.util.Parser;

@Component
public class VariableStringParameterResolver extends AbstractVariableParameterResolver {
    @Override
    public boolean isParameterTypeSupports(Class<?> parameterType) {
        return String.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object castParameterValue(String parameterValue) {
        return parameterValue;
    }
}
