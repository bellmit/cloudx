package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

/**
 * 请求响应状态码，对应参数：$status
 */
public class HttpStatusVariable implements IVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        return String.valueOf(response.getStatus().getCode());
    }
}
