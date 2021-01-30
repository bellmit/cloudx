package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.util.StrUtil;

@Component
public class VariableBooleanParameterResolver extends AbstractVariableParameterResolver {
    @Override
    public boolean isParameterTypeSupports(Class<?> parameterType) {
        return Boolean.TYPE.toString().equals(parameterType.toString());
    }

    @Override
    public Object castParameterValue(String parameterValue) {
        if (!StrUtil.isEmpty(parameterValue)) {
            try {
                return Integer.parseInt(parameterValue);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
