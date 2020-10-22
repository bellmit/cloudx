package cloud.apposs.netkit.server.fai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否为CMD读命令
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadenCmd {
	/**
	 * 写命令在写线程池最多占用多少条执行线程
	 */
	int value() default -1;
}
