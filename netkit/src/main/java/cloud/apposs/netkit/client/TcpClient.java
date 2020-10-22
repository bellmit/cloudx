package cloud.apposs.netkit.client;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TcpClient extends IoClient {
	public TcpClient() {
	}
	
	public TcpClient(ClientConfig config, EventLoopGroup loop)
			throws IOException {
		this(config, loop, SysUtil.random());
	}

	public TcpClient(ClientConfig config, EventLoopGroup loop, int flow)
			throws IOException {
		super(config, loop, flow);
	}

	@Override
	public FutureProcessor newProcessor(InetSocketAddress addr) throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		if (config.isTcpNoDelay()) {
			socketChannel.socket().setTcpNoDelay(true);
		}
		TcpClientPorcessor processor = new TcpClientPorcessor(handler, socketChannel, addr);
		processor.setConfig(config);
		return processor;
	}
}
