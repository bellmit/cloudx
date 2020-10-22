package cloud.apposs.netkit.server.fai.annotation.args;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 获取BODY INTEGER参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgBodyInteger {
	/**
	 * 要匹配的参数KEY
	 */
	int value();

    /**
     * 当客户端没有传递参数的时候开启使用默认值则赋值该参数
     * @return
     */
    int defaultValue() default 0;

    /**
     * 是否使用默认值
     * @return
     */
    boolean useDefault() default false;

}
