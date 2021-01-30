package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

import java.net.InetSocketAddress;

/**
 * 请求远程端口，对应参数：$remote_port
 */
public class RemotePortVariable implements IVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        InetSocketAddress address = (InetSocketAddress) request.getRemoteAddr();
        return String.valueOf(address.getPort());
    }
}
