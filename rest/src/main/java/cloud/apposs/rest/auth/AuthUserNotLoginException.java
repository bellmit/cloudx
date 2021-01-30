package cloud.apposs.rest.auth;

/**
 * 认证用户还没登录
 */
public class AuthUserNotLoginException extends AuthenticationException {
	private static final long serialVersionUID = 3405142463122775034L;

	public AuthUserNotLoginException() {
		super();
	}

	public AuthUserNotLoginException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthUserNotLoginException(String message) {
		super(message);
	}

	public AuthUserNotLoginException(Throwable cause) {
		super(cause);
	}
}
