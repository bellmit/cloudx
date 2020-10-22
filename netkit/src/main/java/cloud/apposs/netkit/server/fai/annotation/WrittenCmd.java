package cloud.apposs.netkit.server.fai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否为CMD写命令，写命令一般服务迁移
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WrittenCmd {
	/**
	 * 写命令在写线程池最多占用多少条执行线程
	 */
	int value() default -1;
}
