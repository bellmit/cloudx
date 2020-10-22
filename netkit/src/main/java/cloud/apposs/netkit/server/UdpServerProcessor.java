package cloud.apposs.netkit.server;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventDatagramChannel;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class UdpServerProcessor extends AbstractIoProcessor {
	private ServerHandler handler;
	
	private EventDatagramChannel channel;
	
	private ServerConfig config;
	
	private ServerHandlerContext context;
	
	public UdpServerProcessor(IoServer server, ServerHandler handler, DatagramChannel channel) {
		this.handler = handler;
		this.channel = new EventDatagramChannel(channel);
		if (context == null) {
			context = new ServerHandlerContext(this, server);
		}
	}
	
	@Override
	public int getBufferSize() {
		return config.getBufferSize();
	}

	@Override
	public boolean isBufferDirect() {
		return config.isBufferDirect();
	}
	
	@Override
	public int getRecvTimeout() {
		return config.getRecvTimeout();
	}

	@Override
	public int getSendTimeout() {
		return config.getSendTimeout();
	}

	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		// UDP协议没有连接状态，在发送数据时就当作已经连接并且发送数据
		// 所以getWelcome在一开始建立连接时发送并无意义，默认直接监听读
		return channel.register(selector, SelectionKey.OP_READ);
	}
	
	@Override
	public EventChannel getChannel() {
		return channel;
	}
	
	@Override
	public void channelRead(Object msg) throws Exception {
		handler.channelRead(context, msg);
	}

	@Override
	public void channelError(Throwable cause) {
		handler.channelError(context, cause);
	}

	@Override
	public void channelClose() {
		handler.channelClose(context);
	}

	public void setConfig(ServerConfig config) {
		this.config = config;
	}
}
