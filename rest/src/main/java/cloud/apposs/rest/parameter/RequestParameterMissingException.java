package cloud.apposs.rest.parameter;

import cloud.apposs.rest.RestException;
import cloud.apposs.rest.annotation.Variable;

import java.lang.annotation.Annotation;

public class RequestParameterMissingException extends RestException {
    private static final long serialVersionUID = -1697457096176021189L;

    private final Parameter parameter;

    public RequestParameterMissingException(Parameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(64);
        message.append("Required ").append(parameter.getType());
        message.append(" parameter");
        Annotation annotation = parameter.getAnnotation();
        if (annotation != null && Variable.class.isAssignableFrom(annotation.annotationType())) {
            message.append(" ").append(((Variable) annotation).value());
        }
        message.append(" of index ").append(parameter.getIndex());
        message.append(" in method[").append(parameter.getMethod().getName()).append("]");
        message.append(" is not present ");
        return message.toString();
    }
}
