package cloud.apposs.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SysUtil {
    private SysUtil() {
    }

    public static int random() {
        // unsigned
        return (int) (Double.doubleToLongBits(Math.random()) & 0x7fffffff);
    }

    public static <T> T checkNotNull(T arg) {
        return checkNotNull(arg, null);
    }

    public static <T> T checkNotNull(T arg, String text) {
        if (arg == null) {
            if (text == null) {
                throw new NullPointerException();
            } else {
                throw new NullPointerException(text);
            }
        }
        return arg;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().equals("");
    }

    public static String lowerFirst(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    public static boolean isIP(String ip) {
        String num = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";
        String regex = "^" + num + "\\." + num + "\\." + num + "\\." + num + "$";
        return match(regex, ip);
    }

    public static boolean match(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static String cutString(String str, int max) {
        return cutString(str, 0, max, "...");
    }

    public static String cutString(String str, int offset, int max) {
        return cutString(str, offset, max, "...");
    }

    /**
     * 裁剪字符串，如果字符串超过长度（max-suffix.length），补suffix
     */
    public static String cutString(String str, int offset, int max, String suffix) {
        if (str.length() - offset <= max) {
            return str.substring(offset, str.length());
        }
        return str.substring(offset, max - suffix.length()) + suffix;
    }

    /**
     * 从输入流中获取字符串
     */
    public static String getStringFromStream(InputStream stream) throws IOException {
        StringBuilder info = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = reader.readLine()) != null) {
            info.append(line);
        }
        return info.toString();
    }

    public static String getPid() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        } catch (Throwable ex) {
            return null;
        }
    }
}
