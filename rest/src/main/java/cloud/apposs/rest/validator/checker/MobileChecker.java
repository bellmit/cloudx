package cloud.apposs.rest.validator.checker;

import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class MobileChecker implements IChecker {
    @Override
    public Object check(Field field, Annotation annotation, Object value) {
        Mobile anno = (Mobile) annotation;
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

        String mobile = value.toString();
        if (!StrUtil.isMobile(mobile)) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("invalid mobile parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        return value;
    }
}
