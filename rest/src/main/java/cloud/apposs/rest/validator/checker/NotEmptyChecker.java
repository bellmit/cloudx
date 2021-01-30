package cloud.apposs.rest.validator.checker;

import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class NotEmptyChecker implements IChecker {
    @Override
    public Object check(Field field, Annotation annotation, Object value) {
        NotEmpty anno = (NotEmpty) annotation;
        if (!anno.require() && value == null) {
            return value;
        }

        if (doCheckEmpty(value)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value;
    }

    private boolean doCheckEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (List.class.isAssignableFrom(value.getClass())) {
            return ((List<?>) value).isEmpty();
        } else if (Map.class.isAssignableFrom(value.getClass())) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return value.toString().isEmpty();
    }
}
