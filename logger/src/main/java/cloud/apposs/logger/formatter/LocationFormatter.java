package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LocationInfo;
import cloud.apposs.logger.LogInfo;

/**
 * 输出日志事件的发生位置，包括类名、方法名、以及在代码中的行数，对应关键字[%l]
 */
public class LocationFormatter extends Formatter {
	public LocationFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		LocationInfo locationInfo = info.getLocationInfo();
		StringBuffer format = new StringBuffer(128);
		format.append(locationInfo.getClassName());
		format.append(".");
		format.append(locationInfo.getMethodName());
		format.append("(");
		format.append(locationInfo.getFileName());
		format.append(":");
		format.append(locationInfo.getLineNumber());
		format.append(")");
		return format.toString();
	}
}
