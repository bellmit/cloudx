package cloud.apposs.rest.auth;

/**
 * 认证不存在异常
 */
public class AuthUserNotFoundException extends AuthenticationException {
	private static final long serialVersionUID = 3405142463122775034L;

	public AuthUserNotFoundException() {
		super();
	}

	public AuthUserNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthUserNotFoundException(String message) {
		super(message);
	}

	public AuthUserNotFoundException(Throwable cause) {
		super(cause);
	}
}
