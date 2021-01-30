package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

import java.net.InetSocketAddress;

/**
 * 请求远程端口，对应参数：$remote_port
 */
public class RemotePortVariable extends AbstractVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        InetSocketAddress address = (InetSocketAddress) request.getRemoteAddr();
        return String.valueOf(address.getPort());
    }
}
