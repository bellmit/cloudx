package cloud.apposs.netkit.filterchain;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;

/**
 *  {@link IoFilter}接口的骨干实现，最大限度地减少了实现此接口所需的工作，子类可扩展此类
 */
public class IoFilterAdaptor implements IoFilter {
	protected String name = "filter-" + this.getClass().getSimpleName();
	
	protected IoFilterAdaptor() {
	}
	
	protected IoFilterAdaptor(String name) {
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void init() {
	}
	
	@Override
	public void destroy() {
	}

	@Override
	public void channelAccept(NextFilter nextFilter, IoProcessor processor,
			EventChannel channel) throws Exception {
		nextFilter.channelAccept(processor, channel);
	}

	@Override
	public void channelConnect(NextFilter nextFilter, IoProcessor processor)
			throws Exception {
		nextFilter.channelConnect(processor);
	}

	@Override
	public void channelRead(NextFilter nextFilter, IoProcessor processor,
			Object message) throws Exception {
		nextFilter.channelRead(processor, message);
	}

	@Override
	public void channelReadEof(NextFilter nextFilter,
			IoProcessor processor, Object message) throws Exception {
		nextFilter.channelReadEof(processor, message);
	}

	@Override
	public void channelSend(NextFilter nextFilter, IoProcessor processor,
			WriteRequest writeRequest) throws Exception {
		nextFilter.channelSend(processor, writeRequest);
	}
	
	@Override
	public void channelClose(NextFilter nextFilter, IoProcessor processor) {
		nextFilter.channelClose(processor);
	}
	
	@Override
	public void filterWrite(NextFilter nextFilter, IoProcessor processor,
			IoBuffer buffer) throws Exception {
		nextFilter.filterWrite(processor, buffer);
	}

	@Override
	public void exceptionCaught(NextFilter nextFilter, IoProcessor processor,
			Throwable cause) {
		nextFilter.exceptionCaught(processor, cause);
	}

	@Override
	public String toString() {
        return name;
    }
}
