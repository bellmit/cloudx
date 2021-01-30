package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求响应状态码，对应参数：$status
 */
public class HttpStatusVariable extends AbstractVariable {
    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        return String.valueOf(response.getStatus());
    }
}
