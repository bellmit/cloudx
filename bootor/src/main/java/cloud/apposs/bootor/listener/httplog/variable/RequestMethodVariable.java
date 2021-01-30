package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

/**
 * 请求远程方法+URL，对应参数：$request
 */
public class RequestMethodVariable extends AbstractVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        return request.getMethod().toUpperCase();
    }
}
