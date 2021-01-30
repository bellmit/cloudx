package cloud.apposs.webx.upload;

import java.io.IOException;

public class FileUploadException extends IOException {
	private static final long serialVersionUID = 1813813709030453359L;

	public FileUploadException() {
		super();
	}

	public FileUploadException(String message, Throwable cause) {
		super(message, cause);
	}

	public FileUploadException(String message) {
		super(message);
	}

	public FileUploadException(Throwable cause) {
		super(cause);
	}
}
