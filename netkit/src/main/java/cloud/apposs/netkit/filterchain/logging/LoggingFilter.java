package cloud.apposs.netkit.filterchain.logging;

import cloud.apposs.logger.Level;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

public class LoggingFilter extends IoFilterAdaptor {
	private final Level level;
	
	public LoggingFilter() {
		this(Level.DEBUG);
	}
	
	public LoggingFilter(Level level) {
		this.level = level;
	}
	
	@Override
	public void channelConnect(IoFilter.NextFilter nextFilter, IoProcessor processor)
			throws Exception {
		Logger.log(level, "Channel[%s] Connect Ok;Flow=%d;",
				processor.getChannel().getRemoteSocketAddress(), processor.getFlow());
        nextFilter.channelConnect(processor);
	}

	@Override
	public void channelRead(NextFilter nextFilter, IoProcessor processor,
			Object msg) throws Exception {
		Logger.log(level, "Channel[%s] Read Ok;Msg=%s;Flow=%d;", 
				processor.getChannel().getRemoteSocketAddress(), msg, processor.getFlow());
        nextFilter.channelRead(processor, msg);
	}

	@Override
	public void filterWrite(NextFilter nextFilter, IoProcessor processor,
			IoBuffer buf) throws Exception {
		Logger.log(level, "Channel[%s] FilterWrite Ok;Buf=%s;Flow=%d;",
				processor.getChannel().getRemoteSocketAddress(), buf, processor.getFlow());
        nextFilter.filterWrite(processor, buf);
	}

	@Override
	public void channelSend(NextFilter nextFilter, IoProcessor processor,
			WriteRequest request) throws Exception {
		Logger.log(level, "Channel[%s] Sended Ok;Req=%s;Flow=%d;",
				processor.getChannel().getRemoteSocketAddress(), request, processor.getFlow());
        nextFilter.channelSend(processor, request);
	}

	@Override
	public void channelAccept(NextFilter nextFilter, IoProcessor processor,
			EventChannel channel) throws Exception {
		Logger.log(level, "Channel[%s] Accept Ok;Flow=%d;", 
				processor.getChannel().getRemoteSocketAddress(), processor.getFlow());
        nextFilter.channelAccept(processor, channel);
	}

	@Override
	public void channelClose(NextFilter nextFilter, IoProcessor processor) {
		Logger.log(level, "Channel[%s] Closed;Flow=%d;",
				processor.getChannel().getRemoteSocketAddress(), processor.getFlow());
		nextFilter.channelClose(processor);
	}

	@Override
	public void exceptionCaught(NextFilter nextFilter, IoProcessor processor,
			Throwable cause) {
		Logger.log(level, cause, "Channel[%s] Exception Caught;Flow=%d;",
				processor.getChannel().getRemoteSocketAddress(), processor.getFlow());
        nextFilter.exceptionCaught(processor, cause);
	}
}
