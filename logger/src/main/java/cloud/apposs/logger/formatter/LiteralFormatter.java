package cloud.apposs.logger.formatter;

import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;

/**
 * 输出代码中指定文本消息
 */
public class LiteralFormatter extends Formatter {
	private String literal;
	
	public LiteralFormatter(String literal) {
		this.literal = literal;
	}
	
	@Override
	public String convert(LogInfo info) {
		return literal;
	}
}
