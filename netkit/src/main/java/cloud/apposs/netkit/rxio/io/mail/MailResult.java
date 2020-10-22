package cloud.apposs.netkit.rxio.io.mail;

public final class MailResult {
	private final boolean success;
	
	private final String response;

	public MailResult(boolean success, String response) {
		this.success = success;
		this.response = response;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getResponse() {
		return response;
	}
}
