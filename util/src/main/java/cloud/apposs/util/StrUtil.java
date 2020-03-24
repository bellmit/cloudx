package cloud.apposs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public final class StrUtil {
	public static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}
	
	/**
	 * 将字符串按指定分隔符拆分成字符数组
	 * 
	 * @param string     字符串
	 * @param delimiters 分隔字符
	 * @param trimTokens 是否截取掉空格字符
	 */
	public static String[] toStringArray(String string, String delimiters, boolean trimTokens) {
		if (string == null) {
			return null;
		}

		StringTokenizer tokenizer = new StringTokenizer(string, delimiters);
		List<String> tokens = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!token.isEmpty()) {
				tokens.add(token);
			}
		}
		return toStringArray(tokens);
	}
	
	public static String[] toStringArray(Collection<String> collection) {
		if (collection == null) {
			return null;
		}

		return collection.toArray(new String[collection.size()]);
	}

	/**
	 * 字符串替换
	 * 
	 * @param inString   原始字符串
	 * @param oldPattern 被替换的正则
	 * @param newPattern 要替换的正则
	 */
	public static String replace(String inString, String oldPattern, String newPattern) {
		if (isEmpty(inString) || isEmpty(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {
			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString, pos, index);
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		sb.append(inString.substring(pos));
		return sb.toString();
	}
	
	public static String formatTimeOutput(long time) {
		if (time > 24 * 60 * 60 * 1000) {
			return (time / (24 * 60 * 60 * 1000)) + " Days";
		} else if (time > 60 * 60 * 1000) {
			return (time / (60 * 60 * 1000)) + " Hours";
		} else if (time > 60 * 1000) {
			return (time / (60 * 1000)) + " Minutes";
		}
		return (time / 1000) + " Seconds";
	}
	
	public static String formatByteOutput(long bytes) {
		if (bytes > 1024 * 1024 * 1024) {
			return (bytes / (1024 * 1024 * 1024)) + " GB";
		} else if (bytes > 1024 * 1024) {
			return (bytes / (1024 * 1024)) + " MB";
		} else if (bytes > 1024) {
			return (bytes / 1024) + " KB";
		}
		return bytes + " Bytes";
	}
}
