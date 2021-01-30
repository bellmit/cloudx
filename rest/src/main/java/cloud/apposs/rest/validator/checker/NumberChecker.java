package cloud.apposs.rest.validator.checker;

import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.security.spec.InvalidParameterSpecException;

public class NumberChecker implements IChecker {
    @Override
    public Object check(Field field, Annotation annotation, Object value) throws Exception {
        Number anno = (Number) annotation;
        if (!anno.require() && value == null) {
            return value;
        }

        if (value == null) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        try {
            int number = Integer.parseInt(value.toString());
            int min = anno.min();
            int max = anno.max();
            if (number < min || number > max) {
                throw new InvalidParameterSpecException(field.getName());
            }
            return number;
        } catch (NumberFormatException e) {
            throw e;
        }
    }
}
