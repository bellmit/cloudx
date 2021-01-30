package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

/**
 * 请求响应状态码，对应参数：$status
 */
public class HttpStatusVariable extends AbstractVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        return String.valueOf(response.getStatus().getCode());
    }
}
