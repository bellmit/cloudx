package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

@Component
public class VariableBooleanParameterResolver extends AbstractVariableParameterResolver {
    @Override
    public boolean isParameterTypeSupports(Class<?> parameterType) {
        return Boolean.TYPE.toString().equals(parameterType.toString());
    }

    @Override
    public Object castParameterValue(String parameterValue) {
        return Parser.parseBoolean(parameterValue, false);
    }
}
