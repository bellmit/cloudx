package cloud.apposs.netkit.server.fai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AidTaskGroup {
	/**
	 * 该指令在哪一个线程组里
	 */
	String group() default "";
	
	int limit() default 1;
}
