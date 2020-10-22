package cloud.apposs.netkit.filterchain.executor;

import cloud.apposs.netkit.IoEvent;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilter.NextFilter;
import cloud.apposs.util.SysUtil;

import java.nio.channels.SelectionKey;

public class IoTask extends AbstractTask {
	private final IoFilter.NextFilter nextFilter;
	
	private final IoProcessor processor;
	
	private final Object parameter;
	
	public IoTask(NextFilter nextFilter, IoProcessor processor, Object parameter) {
		this(nextFilter, processor, parameter, null, -1);
	}
	
	public IoTask(NextFilter nextFilter, IoProcessor processor, Object parameter, String group, int limit) {
		super(processor.getFlow(), group, limit);
		SysUtil.checkNotNull(nextFilter, "nextFilter");
        SysUtil.checkNotNull(processor, "processor");

        this.nextFilter = nextFilter;
        this.processor = processor;
        this.parameter = parameter;
    }

	@Override
	public void run() {
		try {
			nextFilter.channelRead(processor, parameter);
		} catch(Throwable t) {
			nextFilter.exceptionCaught(processor, t);
		} finally {
			// 如果有数据要发送，注册发送事件
			if (!processor.getWriteRequest().isEmpty()) {
				final IoEvent event = processor.getEvent();
				final SelectionKey key = processor.selectionKey();
				// 同时直接在Selector注册写事件马上触发数据发送，因为有可能channelRead不在EventLoop线程内而在于其他线程池，
				// 这样做在于当调用该发送操作的逻辑在另外的线程时也可以立即触发EventLoop线程内的数据发送
				IoEvent.registSelectionKeyEvent(event, key, IoEvent.OP_WRITE);
			}
		}
	}
}
