package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

public interface IVariable {
    /**
     * 解析对应的配置参数，示例：$remote_addr/$remote_port/$http_user_agent等
     */
    String parse(HttpRequest request, HttpResponse response);
}
