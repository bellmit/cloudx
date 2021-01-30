package cloud.apposs.rest.parameter;

import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.validator.Validator;
import cloud.apposs.util.Param;

import java.lang.annotation.Annotation;

/**
 * {@link Model}注解参数绑定
 */
public abstract class BodyParameterResolver<R, P> implements ParameterResolver<R, P> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        Annotation annotation = parameter.getAnnotation();
        if (annotation == null || !Model.class.isAssignableFrom(annotation.annotationType())) {
            return false;
        }

        return true;
    }

    @Override
    public Object resolveArgument(Parameter parameter, R request, P response) throws Exception {
        Param values = getParameterValues(parameter, request, response);
        Class<?> instance = parameter.getType();
        return Validator.deserialize(instance, values);
    }

    /**
     * 获取前端传递的所有参数
     */
    public abstract Param getParameterValues(Parameter parameter, R request, P response) throws Exception;
}
