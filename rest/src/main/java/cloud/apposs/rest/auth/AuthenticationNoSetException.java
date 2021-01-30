package cloud.apposs.rest.auth;

/**
 * 认证未实现异常
 */
public class AuthenticationNoSetException extends AuthenticationException {
	private static final long serialVersionUID = 1401229971493456921L;

	public AuthenticationNoSetException() {
		super();
	}

	public AuthenticationNoSetException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthenticationNoSetException(String message) {
		super(message);
	}

	public AuthenticationNoSetException(Throwable cause) {
		super(cause);
	}
}
