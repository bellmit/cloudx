package cloud.apposs.rest.validator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 参数检查接口
 */
public interface IChecker {
    /**
     * 校验参数是否合法，并且返回对应转码后的值
     *
     * @param  field 参数字段
     * @param  annotation 字段上的注解，和对应的IChecker匹配
     * @param  value 原始值
     * @return 转换后的值
     * @throws Exception 任何参数不合法均抛出对应异常
     */
    Object check(Field field, Annotation annotation, Object value) throws Exception;
}
