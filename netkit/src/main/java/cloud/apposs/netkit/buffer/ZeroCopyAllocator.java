package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ZeroCopyAllocator implements Allocator {
	private int ramsize;
	
	private String cachedir;
	
	public ZeroCopyAllocator(int ramsize, String cachedir) {
		if (ramsize <= 0 || StrUtil.isEmpty(cachedir)) {
			throw new IllegalArgumentException();
		}
		
		this.ramsize = ramsize;
		this.cachedir = cachedir;
	}
	
	@Override
	public IoBuffer allocate(int capacity, boolean direct) {
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		return new FileBuf(capacity, direct, ramsize, cachefile);
	}
	
	@Override
	public IoBuffer wrap(byte[] buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		IoBuffer newBuf = new FileBuf(buf.length, false, ramsize, cachefile);
		newBuf.put(buf, 0, buf.length);
		return newBuf;
	}
	
	@Override
	public IoBuffer wrap(ByteBuffer buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		int total = buf.remaining();
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		IoBuffer newBuf = new FileBuf(total, buf.isDirect(), ramsize, cachefile);
		newBuf.put(buf, buf.position(), total);
		return newBuf;
	}

	@Override
	public void dispose() {
	}
}
