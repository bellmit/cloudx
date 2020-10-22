package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SimpleAllocator implements Allocator {
	@Override
	public IoBuffer allocate(int capacity, boolean direct) {
		return new ByteBuf(capacity, direct);
	}
	
	@Override
	public IoBuffer wrap(byte[] buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		IoBuffer newBuf = new ByteBuf(buf.length, false);
		newBuf.put(buf, 0, buf.length);
		return newBuf;
	}
	
	@Override
	public IoBuffer wrap(ByteBuffer buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		int total = buf.remaining();
		IoBuffer newBuf = new ByteBuf(total, buf.isDirect());
		newBuf.put(buf, buf.position(), total);
		return newBuf;
	}

	@Override
	public void dispose() {
	}
}
