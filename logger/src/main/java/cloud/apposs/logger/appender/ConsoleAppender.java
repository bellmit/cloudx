package cloud.apposs.logger.appender;

import cloud.apposs.logger.Appender;

import java.util.List;

/**
 * 控制台会话输出
 */
public class ConsoleAppender extends Appender {
	@Override
	public void append(List<String> msgList) {
		if (msgList != null) {
			for (String msg : msgList) {
				System.out.print(msg);
			}
		}
	}
}
