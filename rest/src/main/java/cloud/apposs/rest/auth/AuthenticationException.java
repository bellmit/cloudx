package cloud.apposs.rest.auth;

/**
 * 认证异常
 */
public class AuthenticationException extends Exception {
	private static final long serialVersionUID = 1401229971493456920L;

	public AuthenticationException() {
		super();
	}

	public AuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationException(String message) {
		super(message);
	}

	public AuthenticationException(Throwable cause) {
		super(cause);
	}
}
