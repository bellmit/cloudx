package cloud.apposs.netkit.server.websocket;

import cloud.apposs.netkit.filterchain.keepalive.KeepaliveFilter;
import cloud.apposs.netkit.filterchain.websocket.WebSocketFilter;
import cloud.apposs.netkit.server.TcpServer;
import cloud.apposs.netkit.server.http.HttpServerConfig;

public class WebSocketServer extends TcpServer {
    public WebSocketServer(WebSocketConfig config) {
        super(config);
        String charset = config.getCharset();
        filterChain.addFilter(new KeepaliveFilter());
        filterChain.addFilter(new WebSocketFilter(charset, config.isLowerHeaderKey()));
    }
}
