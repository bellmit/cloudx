package cloud.apposs.rest.validator.checker;

import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class NotBlankChecker implements IChecker {
    @Override
    public Object check(Field field, Annotation annotation, Object value) {
        NotBlank anno = (NotBlank) annotation;
        if (!anno.require() && value == null) {
            return value;
        }

        if (StrUtil.isEmpty(value)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value;
    }
}
