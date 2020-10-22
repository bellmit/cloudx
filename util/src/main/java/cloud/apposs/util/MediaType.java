package cloud.apposs.util;

/**
 * Html流媒体类型
 */
public enum MediaType {
	APPLICATION_JSON("application/json"),
	APPLICATION_XML("application/xml"),
	APPLICATION_OCTET_STREAM("application/octet-stream"),
	APPLICATION_FORM_URLENCODED("application/x-www-form-urlencoded"),
	APPLICATION_PDF("application/pdf"),
	IMAGE_GIF("image/gif"),
	IMAGE_JPEG("image/jpeg"),
	IMAGE_PNG("image/png"),
	MULTIPART_FORM_DATA("multipart/form-data"),
	TEXT_HTML("text/html"),
	TEXT_PLAIN("text/plain"),
	TEXT_XML("text/xml"),
	APPLICATION_ALL("*/*");
	
	private final String type;

	private MediaType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	public boolean matchType(String type) {
		if (type == null) {
			return false;
		}
		return type.toLowerCase().startsWith(this.type);
	}

	@Override
	public String toString() {
		return type;
	}
}
