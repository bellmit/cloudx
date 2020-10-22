package cloud.apposs.netkit.client;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;

import java.io.IOException;

public class ClientHandlerContext {
	private IoProcessor processor;
	
	public ClientHandlerContext(IoProcessor processor) {
		this.processor = processor;
	}
	
	public void write(IoBuffer buf) throws IOException {
		processor.write(buf);
	}

	public void write(String str) throws IOException {
		processor.write(str);
	}
	
	public void close() {
		processor.close(true);
	}
}
