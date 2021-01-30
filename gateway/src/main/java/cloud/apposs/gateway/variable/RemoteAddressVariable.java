package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

import java.net.InetSocketAddress;

/**
 * 请求远程地址，对应参数：$remote_addr
 */
public class RemoteAddressVariable implements IVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        InetSocketAddress address = (InetSocketAddress) request.getRemoteAddr();
        return address.getHostString();
    }
}
