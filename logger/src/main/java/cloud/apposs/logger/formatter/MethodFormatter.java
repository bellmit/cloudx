package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LocationInfo;
import cloud.apposs.logger.LogInfo;

/**
 * 输出日志事件的所在的方法名，对应关键字[%M]
 */
public class MethodFormatter extends Formatter {
	public MethodFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		LocationInfo locationInfo = info.getLocationInfo();
		return locationInfo.getMethodName();
	}
}
