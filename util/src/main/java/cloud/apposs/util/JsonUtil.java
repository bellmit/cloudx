package cloud.apposs.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

public class JsonUtil {
    private static final int JSON_CUT_LENGTH = 100;

    /**
     * Json内容解析的各种状态
     */
    /**
     * Json解析初始状态
     */
    private static final int JSON_STATUS_INIT = 0;
    /**
     * Json开始解析状态
     */
    private static final int JSON_STATUS_START = 1;
    /**
     * Json Key以双引号开始的解析
     */
    private static final int JSON_STATUS_KEY_DOUBLE_QUOTE = 2;
    /**
     * Json Key以单引号开始的解析
     */
    private static final int JSON_STATUS_KEY_SINGLE_QUOTE = 3;
    /**
     * Json 冒号的解析
     */
    private static final int JSON_STATUS_COLON = 4;
    /**
     * Json Value的解析
     */
    private static final int JSON_STATUS_VALUE_START = 5;
    /**
     * Json Key-Value的解析结束后解析
     */
    private static final int JSON_STATUS_VALUE_END = 6;
    /**
     * Json解析状态结束
     */
    private static final int JSON_STATUS_FINISH = 7;

    public static Param parseJsonParam(String json) {
        Ref<String> message = new Ref<String>();
        return parseJsonParam(json, message);
    }

    public static Param parseJsonParam(File json) {
        Ref<String> message = new Ref<String>();
        return parseJsonParam(FileUtil.readString(json), message);
    }

    public static Param parseJsonParam(InputStream json) {
        Ref<String> message = new Ref<String>();
        return parseJsonParam(json, message);
    }

    public static Param parseJsonParam(byte[] json, Charset charset) {
        Ref<String> message = new Ref<String>();
        return parseJsonParam(new String(json, charset), message);
    }

    public static Param parseJsonParam(File json, Ref<String> message) {
        Param param = parseJsonParam(FileUtil.readString(json), message);
        return param;
    }

    public static Param parseJsonParam(InputStream json, Ref<String> message) {
        if (json == null) {
            return null;
        }

        StringBuilder jsonStr = new StringBuilder();
        byte[] b = new byte[1024];
        try {
            for (int n; (n = json.read(b)) != -1; ) {
                jsonStr.append(new String(b, 0, n));
            }
            return parseJsonParam(jsonStr.toString(), message);
        } catch (IOException e) {
        }

        return null;
    }

    /**
     * 解析Json数据到Param对象中，
     * 如果解析异常，为了避免异常导致业务影响，不采用抛异常的方式，而是采用错误信息输出的方式给业务方
     *
     * @param json    Json内容
     * @param message 如果Json解析异常则会填充错误信息
     * @return 成功返回Param数据对象
     */
    public static Param parseJsonParam(String json, Ref<String> message) {
        if (StrUtil.isEmpty(json)) {
            return null;
        }

        int length = json.length();
        char letter;
        JsonParseStatus context = new JsonParseStatus();
        Param param = parseJsonValueParam(json, 0, message, context);
        if (param == null) {
            if (message != null && StrUtil.isEmpty(message.value())) {
                message.value(errorMessage("parse json error",
                        context.errnoLineNo, context.errnoLineIdx, json));
            }
            return null;
        }

        // 解析'}'结束之后后面还有数据，视为Json格式异常
        for (int index = context.valueEndIdx + 1; index < length; index++) {
            letter = json.charAt(index);
            calcuateJsonLine(context, json, letter, index);

            if (!isJsonSpaceCharactor(letter)) {
                if (message != null) {
                    message.value(errorMessage("not end with '}' error",
                            context.errnoLineNo, context.errnoLineIdx, json));
                }
                return null;
            }
        }

        return param;
    }

    public static <T> List<T> parseJsonTable(String json) {
        Ref<String> message = new Ref<String>();
        return parseJsonTable(json, message);
    }

    public static <T> Table<T> parseJsonTable(File json) {
        Ref<String> message = new Ref<String>();
        return parseJsonTable(FileUtil.readString(json), message);
    }

    public static <T> Table<T> parseJsonTable(InputStream json) {
        Ref<String> message = new Ref<String>();
        return parseJsonTable(json, message);
    }

    public static <T> Table<T> parseJsonTable(File json, Ref<String> message) {
        return parseJsonTable(FileUtil.readString(json), message);
    }

    public static <T> Table<T> parseJsonTable(InputStream json, Ref<String> message) {
        if (json == null) {
            return null;
        }

        StringBuilder jsonStr = new StringBuilder();
        byte[] b = new byte[4096];
        try {
            for (int n; (n = json.read(b)) != -1; ) {
                jsonStr.append(new String(b, 0, n));
            }
            return parseJsonTable(jsonStr.toString(), message);
        } catch (IOException e) {
        }

        return null;
    }

    /**
     * 解析Json数据到Table对象中，
     * 如果解析异常，为了避免异常导致业务影响，不采用抛异常的方式，而是采用错误信息输出的方式给业务方
     *
     * @param json    Json内容
     * @param message 如果Json解析异常则会填充错误信息
     * @return 成功返回List数据对象
     */
    public static <T> Table<T> parseJsonTable(String json, Ref<String> message) {
        if (StrUtil.isEmpty(json)) {
            return null;
        }

        int length = json.length();
        char letter;
        JsonParseStatus context = new JsonParseStatus();
        Table<T> table = parseJsonValueTable(json, 0, message, context);
        if (table == null) {
            if (message != null && StrUtil.isEmpty(message.value())) {
                message.value(errorMessage("parse json error",
                        context.errnoLineNo, context.errnoLineIdx, json));
            }
            return null;
        }

        // 解析']'结束之后后面还有数据，视为Json格式异常
        for (int index = context.valueEndIdx + 1; index < length; index++) {
            letter = json.charAt(index);
            calcuateJsonLine(context, json, letter, index);

            if (!isJsonSpaceCharactor(letter)) {
                if (message != null) {
                    message.value(errorMessage("not end with ']' error",
                            context.errnoLineNo, context.errnoLineIdx, json));
                }
                return null;
            }
        }

        return table;
    }

    /**
     * 将Object输出成Json需要的格式内容
     *
     * @param format 是否格式化输出
     */
    public static String toJson(Object value, boolean format, int tab, String line) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long ||
                value instanceof Float || value instanceof Double ||
                value instanceof Short || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof String) {
            return "\"" + encodeJson(value.toString()) + "\"";
        }
        if (value instanceof Param) {
            return ((Param) value).toJson(format, tab, line);
        }
        if (value instanceof Table<?>) {
            return ((Table<?>) value).toJson(format, tab, line);
        }
        return "\"" + encodeJson(value.toString()) + "\"";
    }

    /**
     * Json解析过程中的各状态属性维护
     */
    private static final class JsonParseStatus {
        /**
         * 解析Json所对应错误行数
         */
        public int errnoLineNo = 1;
        /**
         * 解析Json所对应错误行的索引位
         */
        public int errnoLineIdx = 0;
        /**
         * 开始解析Json Key所在的全文索引位
         */
        public int keyBegIdx = -1;
        /**
         * 结束解析Json Vlue所在的全文索引位
         */
        public int valueEndIdx = -1;
    }

    /**
     * 统计行数
     */
    private static final void calcuateJsonLine(JsonParseStatus context, String json, char letter, int index) {
        context.errnoLineIdx++;
        if (letter == '\n' && index > 0 && json.charAt(index - 1) == '\r') {
            // \r\n才算作一个换行符
            context.errnoLineNo++;
            context.errnoLineIdx = 0;
        }
    }

    /**
     * 判断是否为Json空白字符
     */
    private static final boolean isJsonSpaceCharactor(char letter) {
        return letter == ' ' || letter == '\t' || letter == '\n' || letter == '\r';
    }

    /**
     * 判断是否为Json结束字符
     */
    private static final boolean isJsonEndCharactor(char letter) {
        return letter == ',' || letter == '}' || letter == ']';
    }

    /**
     * 解析Param对象
     */
    private static Param parseJsonValueParam(String json, int index,
                                             Ref<String> message, JsonParseStatus context) {
        Param param = new Param();
        int length = json.length();
        int status = JSON_STATUS_INIT;
        char letter;
        String key = null;
        try {
            for (; index < length; index++) {
                letter = json.charAt(index);
                calcuateJsonLine(context, json, letter, index);

                switch (status) {
                    case JSON_STATUS_INIT:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        // 检查是否以"{"开头
                        if (letter != '{') {
                            if (message != null) {
                                message.value(errorMessage("not start with '{' error",
                                        context.errnoLineNo, context.errnoLineIdx, json));
                            }
                            return null;
                        }
                        status = JSON_STATUS_START;
                        continue;
                    case JSON_STATUS_START:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        // 是空内容，解析结束了
                        if (letter == '}') {
                            status = JSON_STATUS_FINISH;
                            context.valueEndIdx = index;
                            return param;
                        }

                        // 检查是否以以双引号或者单引号为内容开头
                        if (letter != '\'' && letter != '\"') {
                            if (message != null) {
                                message.value(errorMessage("not start with ''' or '\"' error",
                                        context.errnoLineNo, context.errnoLineIdx, json));
                            }
                            return null;
                        }
                        if (letter == '\"') {
                            status = JSON_STATUS_KEY_DOUBLE_QUOTE;
                        } else {
                            status = JSON_STATUS_KEY_SINGLE_QUOTE;
                        }
                        context.keyBegIdx = index + 1;
                        continue;
                    case JSON_STATUS_KEY_DOUBLE_QUOTE:
                        if (letter == '\\') { // 转义符i+1，跳过一个字符
                            index++;
                            continue;
                        }
                        if (letter == '\"') { // Json Key解析到尾
                            key = decodeJson(json.substring(context.keyBegIdx, index));
                            context.keyBegIdx = -1;
                            status = JSON_STATUS_COLON;
                        }
                        continue;
                    case JSON_STATUS_KEY_SINGLE_QUOTE:
                        if (letter == '\\') { // 转义符i+1，跳过一个字符
                            index++;
                            continue;
                        }
                        if (letter == '\'') { // Json Key解析到尾
                            key = decodeJson(json.substring(context.keyBegIdx, index));
                            context.keyBegIdx = -1;
                            status = JSON_STATUS_COLON;
                        }
                        continue;
                    case JSON_STATUS_COLON:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        if (letter != ':') {
                            if (message != null) {
                                message.value(errorMessage("not contains ':' error",
                                        context.errnoLineNo, context.errnoLineIdx, json));
                            }
                            return null;
                        }
                        status = JSON_STATUS_VALUE_START;
                        continue;
                    case JSON_STATUS_VALUE_START:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        Object value = null;
                        context.errnoLineIdx--;
                        if (letter == '\"' || letter == '\'') {
                            // 解析双引号字符串
                            value = parseJsonValueString(json, index, message, context);
                        } else if (letter == '{') {
                            // 解析Param
                            value = parseJsonValueParam(json, index, message, context);
                        } else if (letter == '[') {
                            // 解析List
                            value = parseJsonValueTable(json, index, message, context);
                        } else {
                            // 解析数字
                            value = parseJsonValueNumber(json, index, message, context);
                        }

                        if (value == null) {
                            if (message != null && StrUtil.isEmpty(message.value())) {
                                message.value(errorMessage("parse json not end with '}' error",
                                        context.errnoLineNo, context.errnoLineIdx, json, null, null, null));
                            }
                            return null;
                        }

                        status = JSON_STATUS_VALUE_END;
                        param.put(key, value);
                        index = context.valueEndIdx;
                        continue;
                    case JSON_STATUS_VALUE_END:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        // 后面还有Key-Value，继续解析
                        if (letter == ',') {
                            status = JSON_STATUS_START;
                            continue;
                        }
                        // 解析结束了
                        if (letter == '}') {
                            status = JSON_STATUS_FINISH;
                            context.valueEndIdx = index;
                            return param;
                        }

                        // 上面两者都不是，Json异常退出
                        if (message != null && StrUtil.isEmpty(message.value())) {
                            message.value(errorMessage("parse json error", context.errnoLineNo, context.errnoLineIdx, json));
                        }
                        return null;
                }

                return param;
            }
        } finally {
            context.valueEndIdx = index;
        }

        // 内容已经结束，但Json还未完全成功解析
        if (status != JSON_STATUS_FINISH) {
            if (message != null && StrUtil.isEmpty(message.value())) {
                message.value(errorMessage("parse json not end with '}' error",
                        context.errnoLineNo, context.errnoLineIdx, json, null, null, null));
            }
        }
        return null;
    }

    /**
     * 解析List对象
     */
    @SuppressWarnings("unchecked")
    private static <T> Table<T> parseJsonValueTable(String json, int start,
                                                    Ref<String> message, JsonParseStatus context) {
        Table<T> table = new Table<T>();
        int length = json.length();
        int status = JSON_STATUS_INIT;
        char letter;
        int index = start;
        try {
            for (index = start; index < length; index++) {
                letter = json.charAt(index);
                calcuateJsonLine(context, json, letter, index);

                switch (status) {
                    case JSON_STATUS_INIT:
                        // 去掉空格和换行符
                        if (letter == ' ' || letter == '\t' || letter == '\n' || letter == '\r') {
                            continue;
                        }

                        // 检查是否以"["开头
                        if (letter != '[') {
                            if (message != null) {
                                message.value(errorMessage("not start with '[' error",
                                        context.errnoLineNo, context.errnoLineIdx, json));
                            }
                            return null;
                        }
                        status = JSON_STATUS_START;
                        continue;
                    case JSON_STATUS_START:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        // 是空内容，解析结束了
                        if (letter == ']') {
                            status = JSON_STATUS_FINISH;
                            context.valueEndIdx = index;
                            return table;
                        }

                        Object value = null;
                        context.errnoLineIdx--;
                        if (letter == '\"' || letter == '\'') {
                            // 解析双引号字符串
                            value = parseJsonValueString(json, index, message, context);
                        } else if (letter == '{') {
                            // 解析Param
                            value = parseJsonValueParam(json, index, message, context);
                        } else if (letter == '[') {
                            // 解析List
                            value = parseJsonValueTable(json, index, message, context);
                        } else {
                            // 解析数字
                            value = parseJsonValueNumber(json, index, message, context);
                        }

                        if (value == null) {
                            if (message != null && StrUtil.isEmpty(message.value())) {
                                message.value(errorMessage("parse json not end with ']' error",
                                        context.errnoLineNo, context.errnoLineIdx, json, null, null, null));
                            }
                            return null;
                        }
                        status = JSON_STATUS_VALUE_END;
                        table.add((T) value);
                        index = context.valueEndIdx;
                        continue;
                    case JSON_STATUS_VALUE_END:
                        // 去掉空格和换行符
                        if (isJsonSpaceCharactor(letter)) {
                            continue;
                        }

                        // 后面还有Value，继续解析
                        if (letter == ',') {
                            status = JSON_STATUS_START;
                            continue;
                        }
                        // 解析结束了
                        if (letter == ']') {
                            status = JSON_STATUS_FINISH;
                            context.valueEndIdx = index;
                            return table;
                        }

                        // 上面两者都不是，Json异常退出
                        if (message != null && StrUtil.isEmpty(message.value())) {
                            message.value(errorMessage("parse json error", context.errnoLineNo, context.errnoLineIdx, json));
                        }
                        return null;
                }

                return table;
            }
        } finally {
            context.valueEndIdx = index;
        }

        // 内容已经结束，但Json还未完全成功解析
        if (status != JSON_STATUS_FINISH) {
            if (message != null && StrUtil.isEmpty(message.value())) {
                message.value(errorMessage("parse json not end with ']' error",
                        context.errnoLineNo, context.errnoLineIdx, json, null, null, null));
            }
            return null;
        }
        return null;
    }

    /**
     * 解析Json双引号字符串值
     */
    private static String parseJsonValueString(String json, int index,
                                               Ref<String> message, JsonParseStatus context) {
        int length = json.length();
        int wordIndex = index + 1;
        int lineIdxStart = context.errnoLineIdx;
        char letter;
        boolean doubleQuote = true;
        try {
            // 先判断是单引号字符串还是双引号字符串
            letter = json.charAt(index);
            if (letter == '\'') {
                doubleQuote = false;
            }
            index++;
            for (; index < length; index++) {
                letter = json.charAt(index);
                calcuateJsonLine(context, json, letter, index);
                if (letter == '\\') { // 转义符i+1，跳过一个字符
                    index++;
                    continue;
                }
                if (doubleQuote && letter == '\"') { // Json Value String解析到尾
                    return decodeJson(json.substring(wordIndex, index));
                } else if (letter == '\'') {
                    return decodeJson(json.substring(wordIndex, index));
                }
            }
        } finally {
            context.valueEndIdx = index;
        }

        // 解析异常，配置异常的开始位置
        context.errnoLineIdx = lineIdxStart;
        return null;
    }

    /**
     * 解析Json数字值
     */
    private static Object parseJsonValueNumber(String json, int index,
                                               Ref<String> message, JsonParseStatus context) {
        int length = json.length();
        char letter;
        int wordIndex = index;
        int lineIdxStart = context.errnoLineIdx;
        String value = null;
        try {
            for (; index < length; index++) {
                letter = json.charAt(index);
                calcuateJsonLine(context, json, letter, index);
                if (isJsonEndCharactor(letter)) { // Json Value Integer解析到尾
                    value = decodeJson(json.substring(wordIndex, index).trim());
                    // 特殊字符回退一个作为valueEndIndex
                    --index;
                    // 有.的解析成double类型，
                    // 没有的先解析成int类型，超过int长度再解析成long类型
                    if (value.equals("true")) {
                        return true;
                    } else if (value.equals("false")) {
                        return false;
                    } else if (value.indexOf(".") > 0) {
                        return Double.parseDouble(value);
                    } else {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return Long.parseLong(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (message != null) {
                message.value(errorMessage("parse json value error",
                        context.errnoLineNo, lineIdxStart, json, null, value, e));
            }
        } finally {
            context.valueEndIdx = index;
        }

        // 解析异常，配置异常的开始位置
        context.errnoLineIdx = lineIdxStart;
        return null;
    }

    private static String errorMessage(String message, int line,
                                       int index, String json) {
        return errorMessage(message, line, index, json, null, null, null);
    }


    private static String errorMessage(String message, int line,
                                       int index, String json, String key, Object value, Exception exp) {
        StringBuilder error = new StringBuilder(128);
        error.append(String.format("%s;line=%d;index=%d;", message, line, index));
        if (key != null) {
            error.append("key=").append(key).append(";");
        }
        if (value != null) {
            error.append("value=").append(value).append(";");
        }
        if (exp != null) {
            error.append("exp=").append(exp.getMessage()).append(";");
        }
        error.append("json=").append(SysUtil.cutString(json, JSON_CUT_LENGTH)).append(";");
        return error.toString();
    }

    /**
     * 把JSON字符串转换为普通字符串，例如\"转换"
     */
    private static String decodeJson(String json) {
        if (json == null) {
            return null;
        }
        int length = json.length();
        // 预留2倍空间，一般情况下够了
        StringBuilder builder = new StringBuilder(length << 1);
        boolean inTrans = false;
        // 用于转换\\uxxxx字符的计数器
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (count != 0) {
                count--;
                continue;
            }

            char c = json.charAt(i);
            if (inTrans) { // 在转义符\中
                switch (c) {
                    case 'b':
                        builder.append('\b');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 'u':
                        int nextIndex = i + 1 + 4;
                        if (nextIndex > json.length()) {
                            break;
                        }
                        count = 4;
                        builder.append((char) Integer.parseInt(json.substring(i + 1, nextIndex), 16));
                        break;
                    case '"':
                    case '\'':
                    case '\\':
                    case '/':
                        builder.append(c);
                        break;
                }
                inTrans = false;
                continue;
            }
            if (c == '\\') {
                inTrans = true;
                continue;
            }
            inTrans = false;
            builder.append(c);
        }
        return builder.toString();
    }

    /**
     * 把字符串转为Json的字符串，如"->\";
     */
    private static String encodeJson(String str) {
        int length = str.length();
        StringBuilder builder = new StringBuilder(length);
        if (str == null) {
            return null;
        }
        int len = str.length();
        boolean inBracket = false;
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '/':
                    if (inBracket) {
                        builder.append("\\/");    // for </script>
                    } else {
                        builder.append("/");
                    }
                    break;
                case '!':
                    if (inBracket) {
                        //buf.append("\\x21");	// for <!-- 这个在ajax到页面拿数据后转parseJSON会出现 Token x的错误
                        builder.append("\\u0021");    // for <!--
                    } else {
                        builder.append("!");    // for <!--
                    }
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                default:
                    //Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((c >= '\u0000' && c <= '\u001F') || (c >= '\u007F' && c <= '\u009F') || (c >= '\u2000' && c <= '\u20FF')) {
                        String ss = Integer.toHexString(c);
                        builder.append("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            builder.append('0');
                        }
                        builder.append(ss.toUpperCase());
                    } else {
                        builder.append(c);
                    }
            }

            inBracket = c == '<';
        }
        return builder.toString();
    }
}
