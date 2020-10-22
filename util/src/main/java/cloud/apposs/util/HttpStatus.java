package cloud.apposs.util;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP状态码，参考：
 * https://www.cnblogs.com/carl10086/p/6185095.html
 */
public enum HttpStatus {
    HTTP_STATUS_200(200, "OK"),
    HTTP_STATUS_204(204, "No Content"),

    HTTP_STATUS_301(301, "Moved Permanently"),
    HTTP_STATUS_302(302, "Found"),
    HTTP_STATUS_304(304, "Not Modified"),

    HTTP_STATUS_404(404, "Not Found"),
    HTTP_STATUS_400(400, "Bad Request"),
    HTTP_STATUS_403(403, "Forbidden"),

    HTTP_STATUS_500(500, "Internal Server Error"),
    HTTP_STATUS_501(501, "Not Implemented"),
    HTTP_STATUS_502(502, "Bad Gateway");

    private final int code;

    private final String description;

    private static final Map<Integer, HttpStatus> status = new HashMap<Integer, HttpStatus>();
    static {
        status.put(HTTP_STATUS_200.getCode(), HTTP_STATUS_200);
        status.put(HTTP_STATUS_204.getCode(), HTTP_STATUS_204);

        status.put(HTTP_STATUS_301.getCode(), HTTP_STATUS_301);
        status.put(HTTP_STATUS_302.getCode(), HTTP_STATUS_302);
        status.put(HTTP_STATUS_304.getCode(), HTTP_STATUS_304);

        status.put(HTTP_STATUS_404.getCode(), HTTP_STATUS_404);
        status.put(HTTP_STATUS_400.getCode(), HTTP_STATUS_400);

        status.put(HTTP_STATUS_500.getCode(), HTTP_STATUS_500);
        status.put(HTTP_STATUS_501.getCode(), HTTP_STATUS_501);
        status.put(HTTP_STATUS_502.getCode(), HTTP_STATUS_502);
    }

    private HttpStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static HttpStatus getStatus(int code) {
        return status.get(code);
    }
}
