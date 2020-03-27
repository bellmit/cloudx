package cloud.apposs.logger;

/**
 * 消息格式转换器
 */
public abstract class Formatter {
    private static String[] SPACES = {
            " ", "  ", "    ", "        ", // 1,2,4,8 spaces
            "                ", // 16 spaces
            "                                " // 32 spaces
        };
    private static String DOT_SUFFIX = "...";

	/** 字符占用最小长度，不足时用空格补齐 */
	private int minChar = -1;

	/** 字符占用最大长度，超过时截取到该最大长度并用...结尾 */
	private int maxChar = 0x7FFFFFFF;

	/** 补充空格是左对齐补充还是右对齐补充 */
	private boolean leftAlign = false;

	protected Formatter() {
	}

	protected Formatter(FormatInfo formatInfo) {
		this.minChar = formatInfo.getMinChar();
		this.maxChar = formatInfo.getMaxChar();
		this.leftAlign = formatInfo.isLeftAlign();
	}

	public void format(StringBuffer buffer, LogInfo info) {
		String convert = convert(info);
        if (convert == null) {
            if (0 < minChar) {
                spacePad(buffer, minChar);
            }
            return;
        }

		int length = convert.length();
		if (length > maxChar) {
		    // 如果超过最大字符则截取并以...结尾
            buffer.append(convert.substring(length-minChar)).append(DOT_SUFFIX);
		} else if (length < minChar) {
		    // 如果小于最小字符则以空格补齐
            if(leftAlign) {
                buffer.append(convert);
                spacePad(buffer, minChar - length);
            } else {
                spacePad(buffer, minChar - length);
                buffer.append(convert);
            }
        } else {
            buffer.append(convert);
        }
	}

    private static void spacePad(StringBuffer buffer, int length) {
        while(length >= 32) {
            buffer.append(SPACES[5]);
            length -= 32;
        }

        for(int i = 4; i >= 0; i--) {
            if((length & (1<<i)) != 0) {
                buffer.append(SPACES[i]);
            }
        }
    }

	public abstract String convert(LogInfo info);
}
