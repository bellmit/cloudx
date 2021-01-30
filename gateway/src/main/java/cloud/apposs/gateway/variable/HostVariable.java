package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

/**
 * 请求远程主机，对应参数：$host
 */
public class HostVariable implements IVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        return request.getRemoteHost();
    }
}
