package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;

/**
 * 输出产生该日志事件的线程名，对应关键字[%h]
 */
public class ThreadNameFormatter extends Formatter {
	public ThreadNameFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		return info.getThreadName();
	}
}
