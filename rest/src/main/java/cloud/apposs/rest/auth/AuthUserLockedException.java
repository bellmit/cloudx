package cloud.apposs.rest.auth;

public class AuthUserLockedException extends AuthenticationException {
	private static final long serialVersionUID = 4244661973464428120L;

	public AuthUserLockedException() {
		super();
	}

	public AuthUserLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthUserLockedException(String message) {
		super(message);
	}

	public AuthUserLockedException(Throwable cause) {
		super(cause);
	}
}
