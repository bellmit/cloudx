package cloud.apposs.rest.validator.checker;

import cloud.apposs.rest.validator.IChecker;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class BoolChecker implements IChecker {
    @Override
    public Object check(Field field, Annotation annotation, Object value) {
        Bool anno = (Bool) annotation;
        // 如果没有传递参数则按注解的默认值返回
        if (!anno.require() && value == null) {
            return anno.value();
        }

        if (value == null) {
            // 输出异常信息
            if (StrUtil.isEmpty(anno.message())) {
                throw new IllegalArgumentException("require parameter " + field.getName());
            } else {
                throw new IllegalArgumentException(anno.message());
            }
        }

        // 校验参数是合法并转换为对象需要的类型值
        String bool = value.toString();
        return Parser.parseBoolean(bool, anno.value());
    }
}
