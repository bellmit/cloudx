package cloud.apposs.netkit.server;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.IoProcessor;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * UDP服务器
 * EPOLL模型下UDP同所有的客户端的通信仅使用一个文件描述符(DataGramSocket)，
 * 所以只能单个线程来处理所有的客户端请求，当然也体现不了EPOLL的优势，
 * UDP协议要求包小于65507BYTE即64K
 */
public class UdpServer extends IoServer {
	public UdpServer(ServerConfig config) {
		super(config, null);
	}
	
	public UdpServer(ServerHandler handler) {
		super(null, handler);
	}
	
	public UdpServer(ServerConfig config, ServerHandler handler) {
		super(config, handler);
	}
	
	@Override
	public void doBind(Selector selector, InetSocketAddress bindAddr) throws IOException {
		DatagramChannel serverChannel = DatagramChannel.open();
		serverChannel.configureBlocking(false);
		DatagramSocket serverSocket = serverChannel.socket();
		serverSocket.setReuseAddress(true);
		try {
			serverSocket.bind(bindAddr);
		} catch (BindException e) {
            Logger.info(e, "port is in used and sleep %d milliseconds to try to rebind",
            		ServerConfig.REBIND_SLEEPTIME);
            try {
				Thread.sleep(ServerConfig.REBIND_SLEEPTIME);
			} catch (InterruptedException ie) {
			}
            serverSocket.bind(bindAddr);
        }
		serverChannel.register(selector, SelectionKey.OP_READ);
	}

	@Override
	public IoProcessor newProcessor(SelectableChannel channel) {
		UdpServerProcessor processor = new UdpServerProcessor(this, handler, (DatagramChannel) channel);
		processor.setConfig(config);
		processor.setListenerSupport(listenerSupport);
		return processor;
	}
	
	@Override
	public void destroy() {
		handler.destroy();
	}
}
