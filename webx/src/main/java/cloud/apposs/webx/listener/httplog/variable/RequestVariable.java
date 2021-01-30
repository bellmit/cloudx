package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求远程方法+URL，对应参数：$request
 */
public class RequestVariable extends AbstractVariable {
    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        String method = request.getMethod().toUpperCase();
        String uri = request.getRequestURI().toString();
        return method + " " + uri;
    }
}
