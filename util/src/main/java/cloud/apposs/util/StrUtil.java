package cloud.apposs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StrUtil {
    public static boolean isEmpty(Object str) {
        return str == null || StrUtil.isEmpty(str.toString());
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(
                "^[a-zA-Z0-9][a-zA-Z0-9_=\\&\\-\\.\\+]*[a-zA-Z0-9]*@[a-zA-Z0-9][a-zA-Z0-9_=\\-\\.]+[a-zA-Z0-9]$");
        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            return false;
        }
        return true;
    }

    public static boolean isMobile(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile("^1\\d{10}$");
        Matcher matcher = pattern.matcher(mobile);
        if (!matcher.matches()) {
            return false;
        }
        return true;
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

    public static String joinArrayString(String[] strings, String joinCharactor, int startIndex) {
        return joinArrayString(strings, joinCharactor, startIndex, strings.length);
    }

    public static String joinArrayString(String[] strings, String joinCharactor) {
        return joinArrayString(strings, joinCharactor, 0, strings.length);
    }

    /**
     * 将字符串数组拉拼接成字符串
     *
     * @param strings 原始字符串数组
     * @param joinCharactor 字符串用什么分隔符
     * @param startIndex 字符串数组开始拼接位置
     * @param endIndex 字符串数组结束拼接位置
     * @return 拼接后的字符串
     */
	public static String joinArrayString(String[] strings, String joinCharactor, int startIndex, int endIndex) {
        if (startIndex < 0 || startIndex > strings.length ||
                endIndex < 0 || endIndex > strings.length || startIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        StringBuilder build = new StringBuilder(32);
        for (int i = startIndex; i < endIndex; i++) {
            String string = strings[i];
            build.append(string.trim());
            if (i < endIndex - 1) {
                build.append(joinCharactor);
            }
        }
        return build.toString();
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

    public static String lowerFirst(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /**
     * 对字符串进行首字母大写，例：app-name->AppName
     */
    public static String upperCamelCase(String string) {
        char letter;
        int length = string.length();
        boolean needUpper = true;
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            letter = string.charAt(index);
            if (needUpper) {
                needUpper = false;
                letter = Character.toUpperCase(letter);
            }
            if (letter == '-' || letter == '_') {
                needUpper = true;
                continue;
            }
            builder.append(letter);
        }
        return builder.toString();
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
