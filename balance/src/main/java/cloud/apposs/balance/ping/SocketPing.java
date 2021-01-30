package cloud.apposs.balance.ping;

import cloud.apposs.balance.IPing;
import cloud.apposs.balance.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于TCP Socket的网络检测
 */
public class SocketPing implements IPing {
	@Override
	public boolean isAlive(Peer peer) {
		Socket socket = null;
	    try {
	        socket = new Socket();
            socket.connect(new InetSocketAddress(peer.getHost(), peer.getPort()), 4000);
            return true;
	    } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
	}
}
