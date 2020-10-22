package cloud.apposs.netkit.client.pool;

public class IoClientException extends Exception {
	private static final long serialVersionUID = -6733511439002271001L;

	public IoClientException() {
		super();
	}

	public IoClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public IoClientException(String message) {
		super(message);
	}

	public IoClientException(Throwable cause) {
		super(cause);
	}
}
