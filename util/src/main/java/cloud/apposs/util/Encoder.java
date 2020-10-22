package cloud.apposs.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

/**
 * 各场景下的字符串转义、解义操作
 */
public class Encoder {
    public static String encodeHtml(String html) {
        if (StrUtil.isEmpty(html)) {
            return html;
        }
        StringBuilder buffer = new StringBuilder(html.length() * 2);
        encodeHtml(html, buffer);
        return buffer.toString();
    }

    /**
     * 把字符串转为写在html中的字符串，避免输出的HTML内容为HTML标签
     * 应用场景：
     * 1、HTML页面输出，例如JSP中的<%=Encoder.encodeHtml(html)%>
     *
     * @param html 原始HTML内容
     * @param buffer 缓存转义后的字符串
     */
    public static void encodeHtml(String html, StringBuilder buffer) {
        try {
            if (html == null) {
                return;
            }
            int spaceCount = 0; // 词/字间 空格数计算器
            int len = html.length();
            for (int i = 0; i < len; ++i) {
                char c = html.charAt(i);
                // 新加, 对于连续空格，第一个空格不处理
                if (c == ' ') { // 遇到空格 计算器+1
                    spaceCount++;
                } else { // 非空格 计算器重置为0
                    spaceCount = 0;
                }

                switch (c) {
                    case '&': {
                        buffer.append("&amp;");
                        break;
                    }
                    // 解决场景：比如</div>word    hei</div>这个在浏览器识别word与hei之间只识别一个空格，所以导致用户输入多个空格无效
                    // 解决办法：将空格转为&nbsp;
                    case ' ': {
                        if (spaceCount < 2) { // 第一个空格不处理
                            buffer.append(c);
                            break;
                        }
                        buffer.append("&nbsp;");
                        break;
                    }
                    case '<': {
                        buffer.append("&lt;");
                        break;
                    }
                    case '>': {
                        buffer.append("&gt;");
                        break;
                    }
                    case '\'': {
                        buffer.append("&#39;");
                        break;
                    }
                    case '\\': {
                        buffer.append("&#92;");
                        break;
                    }
                    case '"': {
                        buffer.append("&quot;");
                        break;
                    }
                    case '\n': {
                        buffer.append("<br/>");
                        break;
                    }
                    case '\r': {
                        break;
                    }
                    case '\t': {
                        break;
                    }
                    default: {
                        buffer.append(c);
                    }
                }
            }
        } catch (Exception e) {
            buffer.setLength(0);
        }
    }

    public static String encodeJavaScript(String html) {
        if (StrUtil.isEmpty(html)) {
            return html;
        }
        StringBuilder buffer = new StringBuilder(html.length() * 2);
        encodeJavaScript(html, buffer);
        return buffer.toString();
    }

    /**
     * 把字符串转换为写在html标签中JavaScript的字符串避免XSS注入
     * 应用场景：
     * 1、请求URL参数拦截过滤，在HttpServletRequest中进行拦截
     * 2、JavaScript标签页面输出，例如JavaScript标签中的var param=<%=Encoder.encodeJavaScript(html)%>
     *
     * @param html 原始内容
     * @param buffer 缓存转义后的字符串
     */
    public static void encodeJavaScript(String html, StringBuilder buffer) {
        try {
            if (html == null) {
                return;
            }
            int len = html.length();
            for (int i = 0; i < len; i++) {
                char c = html.charAt(i);
                // 这里的转换一定要转换为数字形式，而不是\'，因为写在<a
                // href="javascript:alert('xxx')">时，xxx会先被转义导致出错。
                switch (c) {
                    // 以下字符进行斜杠转义
                    case '\\': {
                        buffer.append("\\\\");
                        break;
                    }
                    case '\'': {
                        buffer.append("\\\'");
                        break;
                    }
                    case '`': {
                        buffer.append("\\`");
                        break;
                    }
                    case '"': {
                        buffer.append("\\\"");
                        break;
                    }
                    case '\n': {
                        buffer.append("\\n");
                        break;
                    }
                    case '\r': {
                        break;
                    }
                    case '\t': {
                        break;
                    }
                    case '/': {
                        buffer.append("\\/");
                        break;
                    }
                    // 以下字符转成成ASCII字符串编码
                    case '%': {
                        buffer.append("\\x25");
                        break;
                    }
                    case '<': {
                        buffer.append("\\x3c");
                        break;
                    }
                    case '>': {
                        buffer.append("\\x3e");
                        break;
                    }
                    default: {
                        buffer.append(c);
                    }
                }
            }
        } catch (Exception e) {
            buffer.setLength(0);
        }
    }

    /**
     * BASE64编码
     *
     * @param bytes 原始字节码
     */
    public static String encodeBase64(byte[] bytes) {
        return Base64.encodeBytes(bytes);
    }

    public static String encodeBase64(ByteBuffer buffer) {
         return Base64.encodeBytes(buffer.array(), 0, buffer.limit());
    }

    public static String encodeBase64String(String str) {
        return encodeBase64(str.getBytes());
    }

    public static String encodeBase64Url(ByteBuffer buffer) {
        String base64 = encodeBase64(buffer);
        return toBase64Url(base64);
    }

    public static String encodeBase64Url(byte[] bytes) {
        String base64 = encodeBase64(bytes);
        return toBase64Url(base64);
    }

    /**
     * BASE64解码
     *
     * @param base64 原始Base64字符串
     */
    public static byte[] decodeBase64(String base64) {
        try {
            return Base64.decode(base64);
        } catch (Exception exp) {
            return null;
        }
    }

    public static String decodeBase64String(String base64) {
        byte[] bytes = decodeBase64(base64);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public static String decodeBase64UrlString(String base64) {
        byte[] bytes = decodeBase64Url(base64);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public static byte[] decodeBase64Url(String base64) {
        int len = base64.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            char c = base64.charAt(i);
            // 由于符号"*"在某些邮箱中，会被过滤掉，
            // 这里采用用于URL的改进Base64变种，把"+"替换成"_"，decode时，把"_"替换回"+"
            if (c == '_') {
                sb.append('+');
            } else if (c == '-') {
                sb.append('/');
            } else {
                sb.append(c);
            }
        }
        len = len % 4;
        if (len != 0) {
            for (int i = 0; i < 4 - len; ++i) {
                sb.append('=');
            }
        }
        return decodeBase64(sb.toString());
    }

    public static String encodeUrl(String url) {
        return encodeUrl(url, "UTF-8");
    }

    /**
     * 将请求的URL进行转义
     *
     * @param url URL请求
     * @param charset 字符串编码
     */
    public static String encodeUrl(String url, String charset) {
        if (StrUtil.isEmpty(url)) {
            return url;
        }
        try {
            return URLEncoder.encode(url, charset);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将转义HTML代码转为HTML
     *
     * @param html HTML内容
     */
    public static String decodeHtml(String html){
        try{
            return html.replaceAll("&nbsp;", " ")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&#92;", "\\\\")
                    .replaceAll("&#39;", "\'")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("<br/>", "\n")
                    .replaceAll("&amp;", "&");
        } catch(Exception e) {
            return "";
        }
    }

    public static String decodeUrl(String url) {
        return decodeUrl(url, "utf-8");
    }

    /**
     * 将请求的URL进行解义
     *
     * @param url URL请求
     * @param charset 字符串编码
     */
    public static String decodeUrl(String url, String charset) {
        if (url == null) {
            return "";
        }
        try {
            return URLDecoder.decode(url, charset);
        } catch (Exception e) {
        }
        return "";
    }

    private static String toBase64Url(String base64) {
        int len = base64.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            char c = base64.charAt(i);
            if (c == '+') {
                // 由于符号"*"在某些邮箱中，会被过滤掉，这里采用用于URL的改进Base64变种，把"+"替换成"_"
                //sb.append('*');
                sb.append('_');
            } else if (c == '/') {
                sb.append('-');
            } else if (c == '=') {
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
