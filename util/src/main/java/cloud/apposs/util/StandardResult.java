package cloud.apposs.util;

import java.io.Serializable;

/**
 * 标准错JSON数据返回封装，响应错误码如下
 *{
 *   "code": 0,
 *   "message": "success",
 *   "timestamp": 1590387266300,
 *   "result": {...}
 * }
 * 主要服务于业务响应输出及底层业务异常响应输出，各字段定义如下：
 * 1、code：错误码，必须依据{@link cloud.apposs.util.Errno}
 * 2、message：错误输出，{@link cloud.apposs.util.Errno}会自动输出该错误信息，前端主要是依据该字段来进行错误信息国际化输出
 * 3、timestamp：输出时间戳，前端每次请求时响应的时间戳都必须是最新时间
 * 4、result：响应数据，一般为[]数组或者{}对象
 */
public class StandardResult implements Serializable {
    public static final String CODE = "code";
    public static final String TIMESTAMP = "timestamp";
    public static final String MESSAGE = "message";
    public static final String RESULT = "result";

    /**
     * 响应错误码
     */
    private final Errno errno;

    /**
     * 响应时间戳
     */
    private final long timestamp;

    /**
     * 响应数据
     */
    private final Object result;

    public StandardResult(Errno errno) {
        this(errno, null, System.currentTimeMillis());
    }

    public StandardResult(Errno errno, Object result) {
        this(errno, result, System.currentTimeMillis());
    }

    public StandardResult(Errno errno, Object result, long timestamp) {
        this.errno = errno;
        this.timestamp = timestamp;
        this.result = result;
    }

    public static StandardResult success() {
        return new StandardResult(Errno.OK, null);
    }

    public static StandardResult success(Object data) {
        return new StandardResult(Errno.OK, data);
    }

    public static StandardResult error(Errno errno) {
        return new StandardResult(errno);
    }

    public Errno getErrno() {
        return errno;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Object getResult() {
        return result;
    }

    public boolean isSuccess() {
        return Errno.OK.equals(errno);
    }

    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 将HTTP请求返回的内容数据转换成StandardResult标准输出数据
     */
    public static StandardResult parseHttpParamResult(String content) {
        Param response = JsonUtil.parseJsonParam(content);
        return parseHttpParamResult(response);
    }

    /**
     * 将Param Json数据转换成StandardResult标准输出数据
     */
    public static StandardResult parseHttpParamResult(Param response) {
        int code = response.getInt(CODE);
        String message = response.getString(MESSAGE);
        Object result = response.get(RESULT);
        long timestamp = response.getLong(TIMESTAMP);
        return new StandardResult(new Errno(code, message), result, timestamp);
    }

    /**
     * 将标准输出对象转换成JSON数据用于响应HTML数据输出
     *
     * @return JSON数据
     */
    public String toJson() {
        return toJson(true);
    }

    /**
     * 将标准输出对象转换成JSON数据用于响应HTML数据输出
     *
     * @param  htmlEncode 是否进行HTML编码避免XSS攻击，默认为true
     * @return JSON数据
     */
    public String toJson(boolean htmlEncode) {
        StringBuilder info = new StringBuilder(32);
        info.append("{");
        info.append("\"code\":").append(errno.value()).append(",");
        info.append("\"message\":\"").append(errno.description()).append("\",");
        info.append("\"timestamp\":").append(timestamp).append(",");
        String value = null;
        // 响应的数据进行转码，避免被利用来XSS攻击
        if (result instanceof Param) {
            if (htmlEncode) {
                value = ((Param) result).toHtmlJson();
            } else {
                value = ((Param) result).toJson();
            }
        } else if (result instanceof Table<?>) {
            if (htmlEncode) {
                value = ((Table<?>) result).toHtmlJson();
            } else {
                value = ((Table<?>) result).toJson();
            }
        } else if (result instanceof String) {
            value = "\"" + result.toString() + "\"";
        } else {
            value = result == null ? "{}" : result.toString();
        }
        info.append("\"result\":").append(value);
        info.append("}");
        return info.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
