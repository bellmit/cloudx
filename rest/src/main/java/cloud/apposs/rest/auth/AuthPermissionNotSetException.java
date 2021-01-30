package cloud.apposs.rest.auth;

/**
 * 认证权限没配置异常
 */
public class AuthPermissionNotSetException extends AuthenticationException {
	private static final long serialVersionUID = 3405142463122775034L;

	public AuthPermissionNotSetException() {
		super();
	}

	public AuthPermissionNotSetException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthPermissionNotSetException(String message) {
		super(message);
	}

	public AuthPermissionNotSetException(Throwable cause) {
		super(cause);
	}
}
