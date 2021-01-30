package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求远程主机，对应参数：$host
 */
public class HostVariable extends AbstractVariable {
    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        return request.getServerName();
    }
}
