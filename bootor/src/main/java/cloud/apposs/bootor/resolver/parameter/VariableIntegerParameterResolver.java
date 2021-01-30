package cloud.apposs.bootor.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.util.StrUtil;

@Component
public class VariableIntegerParameterResolver extends AbstractVariableParameterResolver {
    @Override
    public boolean isParameterTypeSupports(Class<?> parameterType) {
        return Integer.TYPE.toString().equals(parameterType.toString());
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
