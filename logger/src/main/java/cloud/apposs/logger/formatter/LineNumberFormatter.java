package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LocationInfo;
import cloud.apposs.logger.LogInfo;

/**
 * 输出日志事件在代码中的行数，对应关键字[%L]
 */
public class LineNumberFormatter extends Formatter {
	public LineNumberFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		LocationInfo locationInfo = info.getLocationInfo();
		return String.valueOf(locationInfo.getLineNumber());
	}
}
