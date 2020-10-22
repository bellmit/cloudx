package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class MappedAllocator implements Allocator {
	private int filesize;
	
	private String cachedir;
	
	public MappedAllocator(int filesize, String cachedir) {
		if (filesize <= 0 || filesize > Integer.MAX_VALUE || StrUtil.isEmpty(cachedir)) {
			throw new IllegalArgumentException();
		}
		
		this.filesize = filesize;
		this.cachedir = cachedir;
	}
	
	@Override
	public IoBuffer allocate(int capacity, boolean direct) throws IOException {
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		return new MappedBuf(cachefile, filesize);
	}
	
	@Override
	public IoBuffer wrap(byte[] buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		IoBuffer newBuf = new MappedBuf(cachefile, filesize);
		newBuf.put(buf, 0, buf.length);
		return newBuf;
	}
	
	@Override
	public IoBuffer wrap(ByteBuffer buf) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		
		int total = buf.remaining();
		String cachefile = cachedir + File.separator + UUID.randomUUID();
		IoBuffer newBuf = new MappedBuf(cachefile, filesize);
		newBuf.put(buf, buf.position(), total);
		return newBuf;
	}

	@Override
	public void dispose() {
	}
}
