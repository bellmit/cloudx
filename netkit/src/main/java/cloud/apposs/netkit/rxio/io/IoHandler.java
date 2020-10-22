package cloud.apposs.netkit.rxio.io;

import java.io.IOException;

public interface IoHandler {
	void channelConnect(IoHandlerContext context) throws IOException;
	
	void channelRead(IoHandlerContext context, Object msg) throws IOException;
}
