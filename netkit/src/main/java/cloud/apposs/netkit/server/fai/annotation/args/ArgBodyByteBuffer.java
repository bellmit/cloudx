package cloud.apposs.netkit.server.fai.annotation.args;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 获取BODY BYTE_BUFFER参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgBodyByteBuffer {
	/**
	 * 要匹配的参数KEY
	 */
	int value();

	boolean useDefault() default false;
}
