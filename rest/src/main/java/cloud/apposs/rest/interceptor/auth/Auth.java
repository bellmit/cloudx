package cloud.apposs.rest.interceptor.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
	/**
	 * 用户权限，值可为ADMIN/UI/CS等，或者UI|UI等多权限合并
	 * 但注意这些值必须在{@link cloud.apposs.rest.auth.AuthManager#getAuthPermissionList()}已经初始化好
	 */
	String value();
}
