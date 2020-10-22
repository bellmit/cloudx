package cloud.apposs.netkit.server;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.IoProcessor;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.*;

/**
 * TCP服务器
 */
public class TcpServer extends IoServer {
	public TcpServer(ServerConfig config) {
		super(config, null);
	}
	
	public TcpServer(ServerHandler handler) {
		super(null, handler);
	}
	
	public TcpServer(ServerConfig config, ServerHandler handler) {
		super(config, handler);
	}

	@Override
	public void doBind(Selector selector, InetSocketAddress bindAddr) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		ServerSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		int backlog = config.getBacklog();
		try {
			serverSocket.bind(bindAddr, backlog);
		} catch (BindException e) {
            Logger.info(e, "port is in used and sleep %d milliseconds to try to rebind",
            		ServerConfig.REBIND_SLEEPTIME);
            try {
				Thread.sleep(ServerConfig.REBIND_SLEEPTIME);
			} catch (InterruptedException ie) {
			}
            serverSocket.bind(bindAddr, backlog);
        }
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	@Override
	public IoProcessor newProcessor(SelectableChannel channel) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) channel;
		SocketChannel clientChannel = serverChannel.accept();
		
    	if (config.isTcpNoDelay()) {
    		clientChannel.socket().setTcpNoDelay(true);
        }
        // 客户端连接请求配置为非阻塞
    	clientChannel.configureBlocking(false);
		TcpServerProcessor processor = new TcpServerProcessor(this, handler, clientChannel);
		processor.setConfig(config);
		processor.setListenerSupport(listenerSupport);
		return processor;
	}

	@Override
	public void destroy() {
		handler.destroy();
	}
}
