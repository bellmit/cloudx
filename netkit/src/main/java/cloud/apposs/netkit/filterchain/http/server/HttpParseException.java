package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.util.HttpStatus;

import java.io.IOException;

public class HttpParseException extends IOException {
	private static final long serialVersionUID = -9002784692607064187L;
	
	private final HttpStatus status;
	
	private final String description;
	
	public HttpParseException(HttpStatus status) {
		this(status, null);
	}
	
	public HttpParseException(HttpStatus status, String description) {
		this.status = status;
		this.description = description;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}
}
