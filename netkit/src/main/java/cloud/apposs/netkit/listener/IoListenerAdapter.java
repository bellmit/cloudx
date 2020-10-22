package cloud.apposs.netkit.listener;

import cloud.apposs.netkit.IoProcessor;

public class IoListenerAdapter implements IoListener {
	@Override
	public void channelAccept(IoProcessor processor) {
	}

	@Override
	public void channelClose(IoProcessor processor) {
	}

	@Override
	public void channelConnect(IoProcessor processor) {
	}

	@Override
	public void channelRead(IoProcessor processor, long readBytesLen) {
	}

	@Override
	public void channelSend(IoProcessor processor, long sendBytesLen) {
	}
	
	@Override
	public void channelError(IoProcessor processor, Throwable t) {
	}
}
