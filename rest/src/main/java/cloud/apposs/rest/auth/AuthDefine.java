package cloud.apposs.rest.auth;

public class AuthDefine {
	/** 超级管理员权限 */
	public static final AuthPermission AUTH_SUPER_ADMIN = new AuthPermission(~0, ~0, "SuperAdmin");
	
	public static final AuthPermission createEmptyAuthPermission() {
		return new AuthPermission(0, 0);
	}
}
