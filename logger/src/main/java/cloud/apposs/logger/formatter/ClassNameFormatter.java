package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LocationInfo;
import cloud.apposs.logger.LogInfo;

/**
 * 输出所属的类目，通常就是所在类的全名，对应关键字[%c]
 */
public class ClassNameFormatter extends Formatter {
	public ClassNameFormatter(FormatInfo formatInfo) {
		super(formatInfo);
	}

	@Override
	public String convert(LogInfo info) {
		LocationInfo locationInfo = info.getLocationInfo();
		return locationInfo.getClassName();
	}
}
