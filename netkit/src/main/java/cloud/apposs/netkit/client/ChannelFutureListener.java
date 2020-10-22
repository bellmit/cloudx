package cloud.apposs.netkit.client;

import java.util.EventListener;

/**
 * 异步Channel执行服务监听
 */
public interface ChannelFutureListener <F extends ChannelFuture> extends EventListener {
	/**
	 * 异步Channel执行成功的监听
	 * 
	 * @param future 异步任务模型
	 */
	void channelComplete(F future);
}
