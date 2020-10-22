package cloud.apposs.netkit.server;

import cloud.apposs.netkit.WriteRequest;

public class ServerHandlerAdaptor implements ServerHandler {
	@Override
	public byte[] getWelcome() {
		return null;
	}
	
	@Override
	public void channelAccept(final ServerHandlerContext context) throws Exception {
	}

	@Override
	public void channelClose(final ServerHandlerContext context) {
	}

	@Override
	public void channelRead(final ServerHandlerContext context, Object message) throws Exception {
	}

	@Override
	public void channelSend(ServerHandlerContext context, WriteRequest request) {
	}

	@Override
	public void channelError(final ServerHandlerContext context, Throwable cause) {
		context.close(true);
	}

	@Override
	public void parseAnnotation(final IoServer server, final Class<? extends ServerHandler> clazz) {
	}

	@Override
	public void destroy() {
	}
}
