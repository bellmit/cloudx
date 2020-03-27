package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;

import java.text.DateFormat;
import java.util.Date;

/**
 *  输出日志时间点的日期或时间，默认格式为ISO8601。
 *  也可以在其后指定格式，比如：%d{yyyy MMM dd HH:mm:ss,SSS}，
 *  输出类似：2002年10月18日 22:10:28，921，
 *  对应关键字[%d]
 */
public class DateFormatter extends Formatter {
	private DateFormat df;
	private Date date;

	public DateFormatter(FormatInfo formatInfo, DateFormat df) {
		super(formatInfo);
		this.date = new Date();
		this.df = df;
	}
	
	@Override
	public String convert(LogInfo info) {
		date.setTime(info.getTimeStamp());
		String format = null;
		try {
			format = df.format(date);
		} catch (Exception e) {
			System.err.println("Error occured while converting date. Exception[" + e + "]");
		}
		return format;
	}
}
