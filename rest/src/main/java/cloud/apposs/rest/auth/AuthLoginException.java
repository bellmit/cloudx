package cloud.apposs.rest.auth;

public class AuthLoginException extends AuthenticationException {
	private static final long serialVersionUID = 4244661973464428120L;

	public AuthLoginException() {
		super();
	}

	public AuthLoginException(String message, Throwable cause) {
		super(message, cause);
	}

	public AuthLoginException(String message) {
		super(message);
	}

	public AuthLoginException(Throwable cause) {
		super(cause);
	}
}
