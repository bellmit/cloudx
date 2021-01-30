package cloud.apposs.netkit.server.http;

import cloud.apposs.netkit.filterchain.http.server.HttpDecryptFilter;
import cloud.apposs.netkit.filterchain.http.server.HttpServerFilter;
import cloud.apposs.netkit.filterchain.keepalive.KeepaliveFilter;
import cloud.apposs.netkit.server.TcpServer;

public class HttpServer extends TcpServer {
    public HttpServer(HttpServerConfig config) {
        super(config);
        String charset = config.getCharset();
        String directory = config.getDirectory();
		filterChain.addFilter(new KeepaliveFilter());
        filterChain.addFilter(new HttpServerFilter(charset, config.isLowerHeaderKey()));
        filterChain.addFilter(new HttpDecryptFilter(charset, directory, config.getMaxFileSize()));
    }
}
