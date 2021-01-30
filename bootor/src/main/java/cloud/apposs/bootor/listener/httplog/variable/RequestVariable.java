package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

/**
 * 请求远程方法+URL，对应参数：$request
 */
public class RequestVariable extends AbstractVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        String method = request.getMethod().toUpperCase();
        String uri = request.getRequestUri();
        return method + " " + uri;
    }
}
