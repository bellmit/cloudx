package cloud.apposs.netkit.client;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TcpClientPorcessor extends FutureProcessor {
	private ClientHandler handler;
	
	protected ChannelFuture future;
	
	private EventSocketChannel channel;
	
	private InetSocketAddress addr;
	
	private ClientConfig config;
	
	public TcpClientPorcessor(ClientHandler handler, SocketChannel socketChannel,
			InetSocketAddress addr) throws IOException {
		this.handler = handler;
		this.future = new ChannelFuture();
		this.channel = new EventSocketChannel(socketChannel);
		this.addr = addr;
		if (context == null) {
			context = new ClientHandlerContext(this);
		}
	}
	
	@Override
	public ChannelFuture getFuture() {
		return future;
	}

	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		channel.connect(addr);
		return channel.register(selector, SelectionKey.OP_CONNECT);
	}
	
	@Override
	public void channelConnect() throws Exception {
		handler.channelConnect((ClientHandlerContext) context);
	}

	@Override
	public void channelRead(Object message) throws Exception {
		handler.channelRead((ClientHandlerContext) context, message);
	}

	@Override
	public void channelError(Throwable cause) {
		handler.channelError(cause);
	}

	@Override
	public void channelClose() {
		// 调用父级服务注销网络句柄，避免一直持有网络句柄导致死循环调用
		super.channelClose();
		future.fireDone();
	}

	@Override
	public EventChannel getChannel() {
		return channel;
	}

	@Override
	public int getConnectTimeout() {
		return config.getConnectTimeout();
	}

	@Override
	public int getRecvTimeout() {
		return config.getRecvTimeout();
	}

	@Override
	public int getSendTimeout() {
		return config.getSendTimeout();
	}

	public void setConfig(ClientConfig config) {
		this.config = config;
	}
}
