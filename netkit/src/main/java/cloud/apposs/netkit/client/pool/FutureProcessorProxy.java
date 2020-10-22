package cloud.apposs.netkit.client.pool;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.client.ChannelFuture;
import cloud.apposs.netkit.client.ClientHandlerContext;
import cloud.apposs.netkit.client.FutureProcessor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

public class FutureProcessorProxy extends FutureProcessor {
	private final IoClientProxy client;
	
	private final FutureProcessor processor;
	
	private boolean connected = false;
	
	/** 
	 * 当前Connection是否被关闭，即被调用{@link #clone()}方法
	 * 此时真正IoClient并未被真正关闭，只是逻辑意义上的关闭
	 * 在下次从数据库连接池取出时仍然可用
	 */
	private AtomicBoolean logicallyClosed = new AtomicBoolean(false);
	
	public FutureProcessorProxy(IoClientProxy client, 
			FutureProcessor processor, IoClientPool pool) throws IOException {
		this.client = client;
		this.processor = processor;
	}
	
	@Override
	public ChannelFuture getFuture() {
		return processor.getFuture();
	}

	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		return processor.doRegister(selector);
	}
	
	@Override
	public void channelConnect() throws Exception {
		this.connected = true;
	}

	@Override
	public void channelRead(Object message) throws Exception {
		client.handler().channelRead((ClientHandlerContext) context, message);
	}

	@Override
	public void channelError(Throwable cause) {
		client.handler().channelError(cause);
	}

	@Override
	public EventChannel getChannel() {
		return processor.getChannel();
	}

	@Override
	public void close(boolean immediately) {
		if (!logicallyClosed.get()) {
			logicallyClosed.set(true);
			client.close();
		}
	}

	protected boolean isConnected() {
		return connected;
	}

	protected void internalClose() {
		processor.close(true);
	}
}
