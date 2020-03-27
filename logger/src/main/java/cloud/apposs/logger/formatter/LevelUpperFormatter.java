package cloud.apposs.logger.formatter;

import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;

/**
 * 输出优先级，大写字符，即DEBUG，INFO，WARN，ERROR，FATAL，对应关键字[%P]
 */
public class LevelUpperFormatter extends Formatter {
	@Override
	public String convert(LogInfo info) {
		return info.getLevel().getString().toUpperCase();
	}
}
