package cloud.apposs.rest.validator.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 被注释的元素必须符合指定的正则表达式
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pattern {
    /**
     * 是否强制需要此值
     */
    boolean require() default true;

    /**
     * 正则表达式
     */
    String regex();

    /**
     * 错误消息输出
     */
    String message() default "";
}
