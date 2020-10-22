package cloud.apposs.netkit.client.pool;

import cloud.apposs.netkit.client.FutureProcessor;
import cloud.apposs.netkit.client.IoClient;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class IoClientProxy extends IoClient {
	private final IoClient client;

	private final IoClientPool pool;
	
	private FutureProcessorProxy processor;
	
	public IoClientProxy(IoClient client, IoClientPool pool) throws IOException {
		this.client = client;
		this.pool = pool;
	}
	
	public IoClientProxy initialize() {
		client.config(config).loop(loop).handler(handler);
		return this;
	}
	
	@Override
	public FutureProcessor newProcessor(InetSocketAddress addr) throws IOException {
		FutureProcessor processor = client.newProcessor(addr);
		this.processor = new FutureProcessorProxy(this, processor, pool);
		return this.processor;
	}
	
	protected FutureProcessorProxy getProcessor() {
		return processor;
	}
	
	/**
	 * 关闭内部网络连接
	 */
	protected void internalClose() {
		if (processor != null) {
			processor.internalClose();
		}
	}

	protected void close() {
		if (processor.selectionKey().isValid()) {
			pool.retriveClient(this);
		}
	}
}
