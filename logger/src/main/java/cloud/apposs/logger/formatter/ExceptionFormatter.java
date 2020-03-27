package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;

/**
 * 输出代码中指定异常消息，对应关键字[%e]
 */
public class ExceptionFormatter extends Formatter {
	public ExceptionFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		String[] throwableInfo = info.getThrowableInfo();
		StringBuffer format = new StringBuffer(256);
		for (String msg : throwableInfo) {
			format.append(msg + System.getProperty("line.separator"));
		}
		return format.toString();
	}
}
