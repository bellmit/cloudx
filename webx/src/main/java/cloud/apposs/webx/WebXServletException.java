package cloud.apposs.webx;

import javax.servlet.ServletException;

public class WebXServletException extends ServletException {
	private static final long serialVersionUID = 869482271838271762L;
	
	private long flow;

	public WebXServletException(long flow) {
		super();
	}

	public WebXServletException(String message, Throwable rootCause, long flow) {
		super(message, rootCause);
	}

	public WebXServletException(String message, long flow) {
		super(message);
	}

	public WebXServletException(Throwable rootCause, long flow) {
		super(rootCause);
	}
	
	@Override
	public String getMessage() {
		return buildMessage(super.getMessage(), getCause(), flow);
	}
	
	private String buildMessage(String message, Throwable cause, long flow) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("WebX Exception is ").append(cause).append(";Flow=").append(flow);
		return sb.toString();
	}
}
