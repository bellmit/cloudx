package cloud.apposs.gateway;

import cloud.apposs.util.HttpStatus;

/**
 * 网关所有异常统一包装
 */
public class GatewayException extends Exception {
    private static final long serialVersionUID = 3106164042454956372L;

    /**
     * HTTP状态码
     */
    private final HttpStatus status;

    public GatewayException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
