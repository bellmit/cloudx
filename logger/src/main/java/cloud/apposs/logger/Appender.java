package cloud.apposs.logger;

import java.util.List;

/**
 * 日志输出渠道
 */
public abstract class Appender {
	public static final String CONSOLE = "console";
	public static final String FILE = "file";
	public static final String DATABASE = "database";
	
	public abstract void append(List<String> msgList);
	
	/**
	 * 关闭渠道，释放资源
	 */
	public void close() {
	}
}
