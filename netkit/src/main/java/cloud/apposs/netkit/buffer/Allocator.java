package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.IoBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Allocator {
	IoBuffer allocate(int capacity, boolean direct) throws IOException;
	
	IoBuffer wrap(byte[] buf) throws IOException;
	
	IoBuffer wrap(ByteBuffer buf) throws IOException;
	
	/**
	 * 释放内存分配器资源
	 */
	void dispose();
}
