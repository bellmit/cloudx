package cloud.apposs.netkit.filterchain.fai;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

public class FaiFilter extends IoFilterAdaptor {
	public static final String FILTER_CONTEXT = "FaiFilterContext";
	
	@Override
	public void channelRead(NextFilter nextFilter,
							IoProcessor processor, Object message) throws Exception {
		if (!(message instanceof IoBuffer)) {
			nextFilter.channelRead(processor, message);
			return;
		}
		
		final FaiProtocol protocol = getFaiProtocol(processor);
		boolean finish = protocol.onRead((IoBuffer) message);
		if (finish) {
			nextFilter.channelRead(processor, protocol);
		}
	}

	@Override
	public void channelSend(NextFilter nextFilter, IoProcessor processor,
			WriteRequest writeRequest) throws Exception {
		FaiProtocol protocol = (FaiProtocol) processor.getAttribute(FILTER_CONTEXT);
		if (protocol != null) {
			protocol.reset();
		}
		nextFilter.channelSend(processor, writeRequest);
	}

	@Override
	public void channelClose(NextFilter nextFilter, IoProcessor processor) {
		FaiProtocol protocol = (FaiProtocol) processor.getAttribute(FILTER_CONTEXT);
		if (protocol != null) {
			protocol.clear();
		}
		nextFilter.channelClose(processor);
	}

	private FaiProtocol getFaiProtocol(IoProcessor processor) {
		FaiProtocol protocol = (FaiProtocol) processor.getAttribute(FILTER_CONTEXT);
        
        if (protocol == null) {
            protocol = new FaiProtocol();
            processor.setAttribute(FILTER_CONTEXT, protocol);
        }
        
        return protocol;
    }
}
