package cloud.apposs.netkit.rxio.io;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;

import java.io.IOException;

public class IoHandlerContext {
	private IoProcessor processor;
	
	public IoHandlerContext(IoProcessor processor) {
		this.processor = processor;
	}
	
	public void write(IoBuffer buf) throws IOException {
		processor.write(buf);
	}

	public void write(String str) throws IOException {
		processor.write(str);
	}
}
