package cloud.apposs.netkit.client;

import cloud.apposs.netkit.AbstractIoProcessor;

public abstract class FutureProcessor extends AbstractIoProcessor {
	public abstract ChannelFuture getFuture();
}
