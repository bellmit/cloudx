package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求远程地址，对应参数：$remote_addr
 */
public class RemoteAddressVariable extends AbstractVariable {
    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        return request.getRemoteAddr();
    }
}
