package cloud.apposs.netkit.server.fai.annotation.args;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 获取Param参数
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgParam {
	Class<?> classDef();
	
	String methodDef();
	
	int keyMatch();

	boolean useDefault() default false;
}
