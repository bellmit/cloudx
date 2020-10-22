package cloud.apposs.netkit.filterchain.cmd;

import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * cmd指令过滤器
 * 主要服务于迁移维护，例如配置的cmd列表则不在服务为readonly状态时不再服务保持数据的一致性
 */
public class WriteCmdFilter<T> extends IoFilterAdaptor {
	private final List<T> cmdList;
	
	private final CmdHandler<T> handler;
	
	public WriteCmdFilter(CmdHandler<T> handler) {
		this.cmdList = new CopyOnWriteArrayList<T>();
		this.handler = handler;
	}
	
	public final void addCmd(T cmd) {
		cmdList.add(cmd);
	}
	
	public final void removeCmd(T cmd) {
		cmdList.remove(cmd);
	}
	
	@Override
	public void channelRead(IoFilter.NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
		T cmd = handler.parseCmd(message);
		if (cmd != null && handler.readonly(cmd)) {
			processor.close(handler.discard(processor, message));
			return;
		}
		nextFilter.channelRead(processor, message);
	}
}
