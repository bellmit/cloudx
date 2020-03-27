package cloud.apposs.logger;

import cloud.apposs.logger.formatter.ClassNameFormatter;
import cloud.apposs.logger.formatter.DateFormatter;
import cloud.apposs.logger.formatter.ErrnoFormatter;
import cloud.apposs.logger.formatter.ExceptionFormatter;
import cloud.apposs.logger.formatter.FileNameFormatter;
import cloud.apposs.logger.formatter.LevelFormatter;
import cloud.apposs.logger.formatter.LevelUpperFormatter;
import cloud.apposs.logger.formatter.LineNumberFormatter;
import cloud.apposs.logger.formatter.LiteralFormatter;
import cloud.apposs.logger.formatter.LocationFormatter;
import cloud.apposs.logger.formatter.LogNameFormatter;
import cloud.apposs.logger.formatter.MessageFormatter;
import cloud.apposs.logger.formatter.MethodFormatter;
import cloud.apposs.logger.formatter.PidFormatter;
import cloud.apposs.logger.formatter.ThreadIdFormatter;
import cloud.apposs.logger.formatter.ThreadNameFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

/**
 * 消息格式管理器
 */
public class FormatParser {
	private static final char ESCAPE_CHAR = '%';
	private static final String LINE_SEP = System.getProperty("line.separator");
	protected static final int BUF_SIZE = 256;
	
	private String format;
	
	private int state = State.LITERAL_STATE;
	
	private int index = 0;
	
	protected final StringBuffer currentLiteral = new StringBuffer(32);
	
	private final StringBuffer output = new StringBuffer(BUF_SIZE);

	private final LinkedList<Formatter> formatList = new LinkedList<Formatter>();

	private final FormatInfo formatInfo = new FormatInfo();

	public class State {
		private static final int LITERAL_STATE = 0;
		private static final int FORMAT_STATE = 1;
		private static final int MINCHAR_STATE = 3;
		private static final int DOTCHAR_STATE = 4;
		private static final int MAXCHAR_STATE = 5;
	}
	
	public FormatParser(String format) {
		this.format = format;
		if (format != null) {
			this.doParseFormat();
		}
	}
	
	public String format(LogInfo info) {
		output.setLength(0);
		for (Formatter format : formatList) {
			format.format(output, info);
		}
		return output.toString();
	}
	
	private void doParseFormat() {
		char c;
		index = 0;
		while (index < format.length()) {
			c = format.charAt(index++);
			switch(state) {
			case State.LITERAL_STATE:
				if(c == ESCAPE_CHAR) {// 匹配关键字%
					switch(format.charAt(index)) {
					// 如果下一个字符还是%的话，不解析
					case ESCAPE_CHAR:
			            currentLiteral.append(c);
			            index++;
			            break;
			        // 如果%后是n，代表换行
					case 'n':
						currentLiteral.append(LINE_SEP);
						index++;
						break;
					default:
						if (currentLiteral.length() != 0) {
							addToList(new LiteralFormatter(currentLiteral.toString()));
						}
						currentLiteral.setLength(0);
						currentLiteral.append(c);
						state = State.FORMAT_STATE;
						formatInfo.reset();
					}
				} else {// 只是普通字符而已
					currentLiteral.append(c);
				}
				break;
			case State.FORMAT_STATE:
				currentLiteral.append(c);
				switch(c) {
					case '-':
						formatInfo.setLeftAlign(true);
						break;
					case '.':
						state = State.DOTCHAR_STATE;
						break;
					default:
						if (c >= '0' && c <= '9') {
							formatInfo.setMinChar(c - '0');
							state = State.MINCHAR_STATE;
						} else {
							finalizeFormatter(c);
						}
				}
				break;
			case State.MINCHAR_STATE:
				currentLiteral.append(c);
				if (c >= '0' && c <= '9') {
					formatInfo.setMinChar(formatInfo.getMinChar() * 10 + (c - '0'));
				} else if(c == '.') {
					state = State.DOTCHAR_STATE;
				} else {
					finalizeFormatter(c);
				}
				break;
			case State.DOTCHAR_STATE:
				currentLiteral.append(c);
				if (c >= '0' && c <= '9') {
					formatInfo.setMaxChar(c - '0');
					state = State.MAXCHAR_STATE;
				} else {
					state = State.LITERAL_STATE;
				}
				break;
			case State.MAXCHAR_STATE:
				currentLiteral.append(c);
				if (c >= '0' && c <= '9') {
					formatInfo.setMaxChar(formatInfo.getMaxChar() * 10 + (c - '0'));
				} else {
					finalizeFormatter(c);
					state = State.LITERAL_STATE;
				}
				break;
			}
		}
		if (currentLiteral.length() != 0) {
			addToList(new LiteralFormatter(currentLiteral.toString()));
		}
	}
	
	private void addToList(Formatter formatter) {
	    formatList.add(formatter);
	    state = State.LITERAL_STATE;
	}
	
	private void finalizeFormatter(char c) {
		Formatter format = null;

		switch(c) {
		case 'm':
			format = new MessageFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'e':
			format = new ExceptionFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'r':
			format = new ErrnoFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'p':
			format = new LevelFormatter();
			currentLiteral.setLength(0);
			break;
		case 'P':
			format = new LevelUpperFormatter();
			currentLiteral.setLength(0);
			break;
		case 'l':
			format = new LocationFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'd':
			DateFormat df;
			String dOpt = extractOption();
			if (dOpt == null) {
				df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			} else {
				df = new SimpleDateFormat(dOpt);
			}
			format = new DateFormatter(formatInfo, df);
			currentLiteral.setLength(0);
			break;
		case 'h':
			format = new ThreadNameFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'i':
			format = new ThreadIdFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'I':
			format = new PidFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'c':
			format = new ClassNameFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'L':
			format = new LineNumberFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'M':
			format = new MethodFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'N':
			format = new LogNameFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		case 'F':
			format = new FileNameFormatter(formatInfo);
			currentLiteral.setLength(0);
			break;
		default:
			System.err.println("Log:Unexpected char [" + c + "] at position " + index
					   + " in format.");
			format = new LiteralFormatter(currentLiteral.toString());
			currentLiteral.setLength(0);
		}
		
		addToList(format);
	}
	
	private String extractOption() {
		if((index < format.length()) && (format.charAt(index) == '{')) {
			int end = format.indexOf('}', index);
			if (end > index) {
				String r = format.substring(index + 1, end);
				index = end+1;
				return r;
			}
		}
		return null;
	}
}
