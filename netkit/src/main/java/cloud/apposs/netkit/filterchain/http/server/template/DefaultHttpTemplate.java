package cloud.apposs.netkit.filterchain.http.server.template;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpParseException;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.StrUtil;

public class DefaultHttpTemplate implements HttpTemplate {
	@Override
	public String generateTemplate(Throwable error) {
		HttpStatus status = HttpStatus.HTTP_STATUS_500;
		if (error instanceof HttpParseException) {
			HttpParseException parseException = ((HttpParseException) error);
			status = parseException.getStatus();
			String description = parseException.getDescription();
			if (!StrUtil.isEmpty(description)) {
				Logger.error(description);
			}
		}
		return String.format("HTTP/1.1 %d %s\r\n\r\n", status.getCode(), status.getDescription());
	}
}
