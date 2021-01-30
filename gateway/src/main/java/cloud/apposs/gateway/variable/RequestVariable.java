package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

/**
 * 请求远程方法+URL，对应参数：$request
 */
public class RequestVariable implements IVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        String method = request.getMethod().toUpperCase();
        String uri = request.getRequestUri();
        return method + " " + uri;
    }
}
