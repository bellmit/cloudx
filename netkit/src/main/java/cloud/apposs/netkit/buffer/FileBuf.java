package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * 文件内存缓存，负责存储接收的网络数据
 * 如果接收的网络字节超过指定配置大小则改成文件存储，同时马上释放接收的网络数据内存
 * 设计此类的目的在于通过零拷贝技术避免网络传输过程中如果传输字节过大例如到达上G时对JVM内存压力过大导致OOM
 * 此类支持无限存储字节，但注意过多的字节拷贝和读取会导致磁盘IO繁忙
 * 
 * 参考：
 * https://my.oschina.net/cloudcoder/blog/299944
 * <br>
 * 内存结构如下：
 * <pre>
 * +-------------------+--------------------+------------------+
 * | discardable bytes |   readable bytes   |  writable bytes  |
 * |                   |(ByteBuffer + File) |                  |
 * +-------------------+--------------------+------------------+
 * |                   |                    |                  |
 * 0      <=        readIdx      <=      writeIdx    <=    capacity
 * </pre>
 */
public class FileBuf implements IoBuffer {
	/** 默认分配内存缓存 */
	public static final int DEFAULT_BUFFER_SIZE = 256 * 1024;
	
	private long writeIdx = 0;
	
	private long readIdx = 0;
	
	/** 当前缓冲最大容量 */
	private long capacity;
	
	/** 接收到的网络数据存储，如果超过配置接收大小则该buf会存储到文件并清空内存数据 */
	private ByteBuffer buffer = null;
	
	/** 接收数据默认分配的内存大小 */
	private int bufsize = DEFAULT_BUFFER_SIZE;
	
	/** 内存缓存是否为DirectByteBuffer，默认为HeapByteBuffer */
	private boolean direct = false;
	
	/** 
	 * 配置允许接收网络数据内存用量，当接收数据大于此时，开始写入磁盘，
	 * 同时每次进行零拷贝传输时传输的数据大小也不能超过此值
	 */
	private int ramsize = Integer.MAX_VALUE;
	
	/** 如果接收的网络字节过大，则会默认存到指定临时文件 */
	private String cachefile = null;
	
	/** 读写文件句柄缓存，避免频繁打开文件句柄导致报文件被占用错误 */
	private volatile FileChannel readfc = null;
	private volatile FileChannel writefc = null;
	
	/** 标记当前文件存储内容容量 */
	private long filesize = 0;
	
	/** 文件读写次数，用于统计 */
	private int fwcnt = 0;
	private int frcnt = 0;
	
	/** 当数据存储到文件时，应用程序读取数据时的内存缓存，为了节省内存，里面的数据会重复覆写 */
	private CachedBuffer cachedbuffer = null;
	
	public FileBuf(int bufsize, boolean direct) {
		this(bufsize, direct, Integer.MAX_VALUE, null);
	}
	
	public FileBuf(int bufsize, boolean direct, String cachefile) {
		this(bufsize, direct, bufsize, cachefile);
	}
	
	public FileBuf(int bufsize, boolean direct, int ramsize, String cachefile) {
		if (bufsize <= 0 || ramsize <= 0) {
			throw new IllegalArgumentException();
		}
		this.capacity = bufsize;
		this.bufsize = bufsize;
		this.direct = direct;
		this.ramsize = ramsize;
		this.cachefile = cachefile;
	}
	
	@Override
	public long readIdx() {
		return readIdx;
	}
	
	@Override
	public long writeIdx() {
		return writeIdx;
	}
	
	@Override
	public long capacity() {
		return capacity;
	}

	@Override
	public IoBuffer readIdx(long readIdx) {
		if (readIdx < 0 || readIdx > writeIdx) {
            throw new IndexOutOfBoundsException(String.format(
                    "readerIndex: %d (expected: 0 <= readerIndex <= writerIndex(%d))", readIdx, writeIdx));
        }
        this.readIdx = readIdx;
        return this;
	}
	
	@Override
	public IoBuffer writeIdx(long writeIdx) {
		if (writeIdx < readIdx || writeIdx > capacity()) {
            throw new IndexOutOfBoundsException(String.format(
                    "writerIndex: %d (expected: readerIndex(%d) <= writerIndex <= capacity(%d))",
                    writeIdx, readIdx, capacity()));
        }
        this.writeIdx = writeIdx;
        return this;
	}
	
	@Override
	public long readableBytes() {
		return writeIdx - readIdx;
	}

	@Override
	public long writableBytes() {
		return capacity() - writeIdx;
	}

	@Override
	public boolean hasReadableBytes() {
		return readableBytes() > 0;
	}

	@Override
	public boolean hasWritableBytes() {
		return writableBytes() > 0;
	}

	@Override
	public byte get() throws IOException {
		if (readIdx >= writeIdx) {
			throw new IndexOutOfBoundsException();
		}
		
		return get(readIdx++);
	}

	@Override
	public byte get(long index) throws IOException {
		if (index < 0 || index > writeIdx) {
			throw new IndexOutOfBoundsException();
		}
		
		// 读取的位置有可能在文件里面也有可能在内存buffer里面
		if (isFileWritable()) {
			if (index < filesize) {
				if (cachedbuffer == null) {
					cachedbuffer = new CachedBuffer();
				}
				return cachedbuffer.get(index);
			}
			index = index - filesize;
		}
		if (buffer == null || buffer.limit() < index) {
		    throw new IndexOutOfBoundsException();
		}
		return buffer.get((int) index);
	}

	@Override
	public IoBuffer get(byte[] dst) throws IOException {
		return get(dst, 0, dst.length);
	}
	
	@Override
	public IoBuffer get(byte[] dst, long offset, int length) throws IOException {
		if (offset < 0 || length < 0 || length > writeIdx) {
			throw new IndexOutOfBoundsException();
		}

		int index = 0;
		long end = offset + length;
		for (long i = offset; i < end; i++) {
			dst[index++] = get(i);
		}
		readIdx += length;
		return this;
	}

	@Override
	public byte[] array() throws IOException {
		if (readableBytes() > Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException();
		}
		
		return array(readIdx, (int) readableBytes());
	}
	
	@Override
	public byte[] array(long offset, int length) throws IOException {
		if (offset < 0 || length < 0 || (offset + length) > writeIdx) {
			throw new IndexOutOfBoundsException();
		}

		int index = 0;
		long end = offset + length;
		byte[] array = new byte[length];
		for (long i = offset; i < end; i++) {
			array[index++] = get(i);
		}
		
		return array;
	}

	@Override
	public String string() {
		if (readableBytes() > Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException();
		}
		
		return string(readIdx, (int) readableBytes(), Charset.forName("utf-8"));
	}

	@Override
	public String string(Charset charset) {
		if (readableBytes() > Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException();
		}
		
		return string(readIdx, (int) readableBytes(), charset);
	}

	@Override
	public String string(long offset, int length) {
		return string(offset, length, Charset.forName("utf-8"));
	}
	
	@Override
	public String string(long offset, int length, Charset charset) {
		if (offset < 0 || length < 0 || (offset + length) > writeIdx) {
			throw new IndexOutOfBoundsException();
		}
		
		try {
			return new String(array(offset, length), 0, length, charset);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public ByteBuffer buffer() throws IOException {
		if (readableBytes() > Integer.MAX_VALUE) {
			throw new IndexOutOfBoundsException();
		}
		
		return buffer(readIdx, (int) readableBytes(), false);
	}

	@Override
	public ByteBuffer buffer(long offset, int length) throws IOException {
		return buffer(offset, length, false);
	}

	@Override
	public ByteBuffer buffer(long offset, int length, boolean direct) throws IOException {
		if (offset < 0 || length < 0 || (offset + length) > writeIdx) {
			throw new IndexOutOfBoundsException();
		}
		
		ByteBuffer newBuf = null;
		if (direct) {
			newBuf = ByteBuffer.allocateDirect(length);
		} else {
			newBuf = ByteBuffer.allocate(length);
		}
		long end = offset + length;
		for (long i = offset; i < end; i++) {
			newBuf.put(get(i));
		}
		
		return newBuf;
	}

	@Override
	public IoBuffer put(byte b) throws IOException {
		// 一个字节的直接写到内存即可
		autoExpandIfBufferFull(1);
		buffer.put(b);
		doAdjustProperty(1, -1);
		return this;
	}

	@Override
	public IoBuffer put(byte[] src) throws IOException {
		return put(src, 0, src.length);
	}
	
	@Override
	public IoBuffer put(byte[] src, int offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.length) {
			throw new IndexOutOfBoundsException();
		}
		
		long total = buffer == null ? length : buffer.position() + length;
		// 如果要填充的内存数据大于最大配置内存阀值则改成写入文件，否则写入内存
		if (flushable(total)) {
			final FileChannel fc = getWritefc();
			flushToFile(fc);
			flushToFile(src, offset, length, fc);
		} else {
			flushToRam(src, offset, length);
		}
		
		return this;
	}

	@Override
	public IoBuffer put(ByteBuffer src) throws IOException {
		return put(src, src.position(), src.remaining());
	}

	@Override
	public IoBuffer put(ByteBuffer src, int offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.limit()) {
			throw new IndexOutOfBoundsException();
		}
		
		long total = buffer == null ? length : buffer.position() + length;
		// 如果要填充的内存数据大于最大配置内存阀值则改成写入文件，否则写入内存
		if (flushable(total)) {
			final FileChannel fc = getWritefc();
			flushToFile(fc);
			flushToFile(src, offset, length, fc);
		} else {
			flushToRam(src, offset, length);
		}
		src.position(offset + length);
		
		return this;
	}
	
	@Override
	public IoBuffer put(String src) throws IOException {
		return put(src, Charset.forName("utf-8"));
	}
	
	@Override
	public IoBuffer put(String src, Charset charset) throws IOException {
		return put(ByteBuffer.wrap(src.getBytes(charset)));
	}

	@Override
	public IoBuffer put(IoBuffer src) throws IOException {
		return put(src, src.readIdx(), src.readableBytes());
	}
	
	@Override
	public IoBuffer put(IoBuffer src, long offset, long length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.writeIdx()) {
			throw new IndexOutOfBoundsException();
		}
		
		long total = buffer == null ? length : buffer.position() + length;
		// 如果要填充的内存数据大于最大配置内存阀值则改成写入文件，否则写入内存
		if (flushable(total)) {
			final FileChannel fc = getWritefc();
			flushToFile(fc);
			flushToFile(src, offset, length, fc);
		} else {
			flushToRam(src, (int) offset, (int) length);
		}
		src.readIdx(offset + length);
		
		return this;
	}
	
	@Override
	public IoBuffer put(File src) throws IOException {
		return put(src, 0, src.length());
	}
	
	@Override
	public IoBuffer put(File src, long offset, long length) throws IOException {
		if (src == null || offset < 0 || length < 0 || offset > length) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.length()) {
			throw new IndexOutOfBoundsException();
		}
		
		FileChannel channel = null;
		try {
			channel = new FileInputStream(src).getChannel();
			return put(channel, offset, length);
		} finally {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e) {
				}
				channel = null;
			}
		}
	}
	
	@Override
	public IoBuffer put(FileChannel src) throws IOException {
		return put(src, 0, src.size());
	}
	
	@Override
	public IoBuffer put(FileChannel src, long offset, long length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.size()) {
			throw new IndexOutOfBoundsException();
		}
		
		long total = buffer == null ? length : buffer.position() + length;
		// 如果要填充的内存数据大于最大配置内存阀值则改成写入文件，否则写入内存
		if (flushable(total)) {
			final FileChannel fc = getWritefc();
			flushToFile(fc);
			flushToFile(src, offset, length, fc);
		} else {
			flushToRam(src, offset, (int) length);
		}
		
		return this;
	}

	@Override
	public long channelRecv(EventChannel channel) throws IOException {
		SysUtil.checkNotNull(channel, "channel");
		
		int recvLen = 0;
		if (isFileWritable()) {
			final FileChannel fc = getWritefc();
			// 先把内存数据刷到文件，直接零拷贝读取网络数据
			if (buffer != null) {
				flushToFile(fc);
			}
			recvLen = (int) channel.transferTo(fc, writeIdx, ramsize);
			// 远端服务有可能主动关闭连接，那么recvLen则会返回-1
			if (recvLen > 0) {
				doAdjustProperty(recvLen, recvLen);
			}
		} else {
			// 未到达最大内存上限阀值，通过内存读取网络数据
			autoExpandIfBufferFull();
			recvLen = channel.recv(buffer);
			if (flushable()) {
				final FileChannel fc = getWritefc();
				// 超过配置最大内存则刷入磁盘，下次直接通过零拷贝读取网络数据
				flushToFile(fc);
			}
			// 远端服务有可能主动关闭连接，那么recvLen则会返回-1
			if (recvLen > 0) {
				doAdjustProperty(recvLen, -1);
			}
		}
		
		return recvLen;
	}

	@Override
	public long channelSend(EventChannel channel) throws IOException {
		SysUtil.checkNotNull(channel, "channel");
		
		// 判断所有数据是否发送完毕
		if (!hasReadableBytes()) {
			return 0;
		}
		
		int sendLen = 0;
		// 如果有磁盘缓存文件要发送，先将文件数据发送到网络
		if (isFileReadable()) {
			final FileChannel fc = getReadfc();
			// 零拷贝将文件数据发送到网络，避免一开始传输量过大也会导致OOM
			int trans = (int) (ramsize > (filesize - readIdx) ? (filesize - readIdx) : ramsize);
			sendLen = (int) channel.transferFrom(fc, readIdx, trans);
			if (sendLen > 0) {
				readIdx += sendLen;
				frcnt++;
			}
		} else {
			// 再将内存数据发送到网络
			if (buffer != null) {
				buffer.flip();
				sendLen = channel.send(buffer);
				if (sendLen > 0) {
					// 更新position状态
					readIdx += sendLen;
				}
			}
		}
		
		return sendLen;
	}

	private boolean flushable() {
		int total = buffer == null ? 0 : buffer.position();
		return flushable(total);
	}
	
	/**
	 * 判断内存缓存数据是否需要刷入磁盘
	 */
	private boolean flushable(long size) {
		return !StrUtil.isEmpty(cachefile) && ramsize > 0 && size >= ramsize;
	}
	
	/**
	 * 判断是否可以直接写硬盘
	 */
	private boolean isFileWritable() {
		return writefc != null || flushable();
	}
	
	/**
	 * 判断是否可以直接读硬盘
	 */
	private boolean isFileReadable() {
		return writefc != null && readIdx < filesize;
	}
	
	/**
	 * 将buffer内存数据刷入磁盘文件
	 */
	private long flushToFile(FileChannel dst) throws IOException {
		if (dst == null) {
			throw new IllegalArgumentException("dst");
		}
		if (buffer == null) {
			return 0;
		}
		
		buffer.flip();
		int size = buffer.remaining();
		if (size <= 0) {
			buffer = null;
			return 0;
		}
		while (buffer.hasRemaining()) {
			int length = dst.write(buffer, dst.position());
			if (length > 0) {
				dst.position(dst.position() + length);
				filesize += length;
				fwcnt++;
			}
		}
		buffer = null;
		
		return size;
	}
	
	/**
	 * 将src内存数据刷入磁盘文件
	 */
	private long flushToFile(ByteBuffer src, int offset, int length, 
			FileChannel dst) throws IOException {
		if (src == null || dst == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.limit()) {
			throw new IndexOutOfBoundsException("length");
		}
		
		int oldPos = src.position();
		int oldLimit = src.limit();
		try {
			src.position(offset);
			src.limit(offset + length);
			while (src.hasRemaining()) {
				int size = dst.write(src, dst.position());
				if (size > 0) {
					dst.position(dst.position() + size);
					doAdjustProperty(size, size);
				}
			}
		} finally {
			src.limit(oldLimit);
			src.position(oldPos);
		}

		return length;
	}
	
	/**
	 * 将src内存数据刷入磁盘文件
	 */
	private long flushToFile(FileChannel src, long offset, long length, 
			FileChannel dst) throws IOException {
		if (src == null || dst == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.size()) {
			throw new IndexOutOfBoundsException("length");
		}
		
		int total = 0;
		while (total < length) {
			// 文件之间的传输也要配置一次传输缓冲，避免一开始传输量过大也会导致OOM
			int needTrans = (int) (ramsize > (length - total) ? (length - total) : ramsize);
			long transLen = src.transferTo(offset, needTrans, dst);
			total += transLen;
			offset += transLen;
			doAdjustProperty(transLen, transLen);
		}

		return length;
	}
	
	/**
	 * 将src内存数据刷入磁盘文件
	 */
	private long flushToFile(IoBuffer src, long offset, long length, 
			FileChannel dst) throws IOException {
		if (src == null || dst == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.writeIdx()) {
			throw new IndexOutOfBoundsException("length");
		}
		
		if (src instanceof FileBuf) {
			FileBuf fb = (FileBuf) src;
			if (fb.isFileReadable()) {
				// 文件内存缓存采用零拷贝方式来提升数据复制
				flushToFile(fb.getReadfc(), offset, length, dst);
			} else if (fb.buffer != null) {
				flushToFile(fb.buffer, (int) offset, (int) length, dst);
			}
		} else {
			ByteBuffer buf = src.buffer(offset, (int) length);
			buf.flip();
			while (buf.hasRemaining()) {
				int size = dst.write(buf, dst.position());
				if (size > 0) {
					dst.position(dst.position() + size);
					doAdjustProperty(size, size);
				}
			}
		}

		return length;
	}
	
	/**
	 * 将src内存数据刷入磁盘文件
	 */
	private long flushToFile(byte[] src, int offset, int length, 
			FileChannel dst) throws IOException {
		if (src == null || offset < 0 || length < 0 || dst == null) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.length) {
			throw new IndexOutOfBoundsException("length");
		}
		
		ByteBuffer buf = ByteBuffer.wrap(src, offset, length);
		while (buf.hasRemaining()) {
			int size = dst.write(buf, dst.position());
			if (size > 0) {
				dst.position(dst.position() + size);
				doAdjustProperty(size, size);
			}
		}

		return length;
	}
	
	/**
	 * 将src内存数据刷入内存缓存
	 */
	private int flushToRam(ByteBuffer src, int offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.limit()) {
			throw new IndexOutOfBoundsException("length");
		}
		
		autoExpandIfBufferFull(length);
		int oldPos = src.position();
		int oldLimit = src.limit();
		try {
			src.position(offset);
			src.limit(offset + length);
			buffer.put(src);
		} finally {
			src.limit(oldLimit);
			src.position(oldPos);
		}
		
		doAdjustProperty(length, -1);
		return length;
	}
	
	/**
	 * 将src文件管道刷入内存缓存
	 */
	private int flushToRam(FileChannel src, long offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if ((offset + length) > src.size()) {
			throw new IndexOutOfBoundsException("length");
		}
		
		autoExpandIfBufferFull(length);
		int limit = (int) (buffer.limit() > length ? length : buffer.limit());
		buffer.limit(limit);
		int readLen = 0;
		while (readLen < limit) {
			readLen += src.read(buffer, offset);
			offset += readLen;
		}
		
		doAdjustProperty(length, -1);
		return length;
	}
	
	/**
	 * 将src内存数据刷入内存缓存
	 */
	private int flushToRam(byte[] src, int offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if (src.length - offset < length) {
			throw new IllegalArgumentException("length over limit");
		}
		
		autoExpandIfBufferFull(length);
		buffer.put(src, offset, length);
		doAdjustProperty(length, -1);
		return length;
	}
	
	/**
	 * 将src内存数据刷入内存缓存
	 */
	private int flushToRam(IoBuffer src, int offset, int length) throws IOException {
		if (src == null || offset < 0 || length < 0) {
			throw new IllegalArgumentException();
		}
		if (src.writeIdx() - offset < length) {
			throw new IllegalArgumentException("length over limit");
		}
		
		autoExpandIfBufferFull(length);
		ByteBuffer buf = src.buffer(offset, length);
		buf.flip();
		buffer.put(buf);
		
		doAdjustProperty(length, -1);
		return length;
	}
	
	private void autoExpandIfBufferFull() {
		autoExpandIfBufferFull(0);
	}
	
	/**
	 * 在所需空间不足情况下自动扩展空间容量
	 */
	private void autoExpandIfBufferFull(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("size");
		}
		
		initialBufferIfNull();
		int total = buffer.position() + size;
		int bufcap = buffer.capacity();
		if (total <= bufcap) {
			return;
		}
		
		int bufsize = bufcap << 1;
		int allosize = bufsize > total ? bufsize : total;
		ByteBuffer newBuf = null;
		if (direct) {
			newBuf = ByteBuffer.allocateDirect(allosize);
		} else {
			newBuf = ByteBuffer.allocate(allosize);
		}
		buffer.flip();
		newBuf.put(buffer);
		buffer = newBuf;
		capacity += (allosize - bufcap);
	}
	
	/**
	 * 初始化内存缓存
	 */
	private void initialBufferIfNull() {
		if (buffer != null) {
			return;
		}
		if (direct) {
			buffer = ByteBuffer.allocateDirect(bufsize);
		} else {
			buffer = ByteBuffer.allocate(bufsize);
		}
	}
	
	/**
	 * 获取读取文件的句柄
	 */
	private synchronized FileChannel getReadfc() throws IOException {
		if (readfc == null) {
			readfc = openCacheFile(false, false);
		}
		return readfc;
	}

	/**
	 * 获取写入文件的句柄
	 */
	private synchronized FileChannel getWritefc() throws IOException {
		if (writefc == null) {
			writefc = openCacheFile(true, false);
		}
		return writefc;
	}
	
	/**
	 * 打开缓存文件
	 * 
	 * @param writemode 是写入文件还是读取文件，true为写入文件，false为读取文件
	 */
	private FileChannel openCacheFile(boolean writemode, boolean append) throws IOException {
		if (writemode) {
			// 以内容追加的形式打开文件
			return new FileOutputStream(cachefile, append).getChannel();
		}
		return new FileInputStream(cachefile).getChannel();
	}
	
	@Override
	public long flush(String file) throws IOException {
		return flush(new File(file));
	}

	@Override
	public long flush(File file) throws IOException {
		final FileChannel channel = new FileOutputStream(file, false).getChannel();
		try {
			long total = 0;
			if (isFileReadable()) {
				final FileChannel fc = getReadfc();
				total += flushToFile(fc, 0, fc.size(), channel);
			}
			total += flushToFile(channel);
			return total;
		} finally {
			channel.close();
		}
	}
	
	@Override
	public void expand(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("length");
		}
		autoExpandIfBufferFull(length);
	}
	
	@Override
	public void reset() {
		doResetProperty();
		// 释放文件句柄资源
		if (readfc != null) {
			try {
				readfc.position(0);
			} catch (IOException e) {
			}
		}
		if (writefc != null) {
			try {
				writefc.position(0);
			} catch (IOException e) {
			}
		}
		// 清空内存缓存
		if (buffer != null) {
			buffer.clear();
		}
		if (cachedbuffer != null) {
			cachedbuffer.clear();
		}
	}

	@Override
	public void free() {
		boolean fileCreated = false;
		// 释放文件句柄资源
		if (readfc != null) {
			fileCreated = true;
			try {
				readfc.close();
			} catch (IOException e) {
			}
			readfc = null;
		}
		if (writefc != null) {
			fileCreated = true;
			try {
				writefc.close();
			} catch (IOException e) {
			}
			writefc = null;
		}
		// 删除缓存文件
		if (cachefile != null && fileCreated) {
			File file = new File(cachefile);
			if (file.exists()) {
				file.delete();
			}
			cachefile = null;
		}
		// 清空内存缓存
		if (buffer != null) {
			buffer.clear();
			buffer = null;
		}
		doResetProperty();
	}
	
	private void doResetProperty() {
		filesize = 0;
		frcnt = 0;
		fwcnt = 0;
		readIdx = 0;
		writeIdx = 0;
	}
	
	private void doAdjustProperty(long length, long filesize) {
		if (filesize > 0) {
			this.filesize += filesize;
			fwcnt += 1;
		}
		writeIdx += length;
		capacity = capacity < writeIdx ? writeIdx : capacity;
	}
	
	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
		info.append(getClass().getSimpleName() + "[");
		info.append("capacity=" + capacity());
		info.append(" readIdx=" + readIdx());
		info.append(" writeIdx=" + writeIdx());
		info.append(" frcnt=" + frcnt);
		info.append(" fwcnt=" + fwcnt);
		info.append(" buffer=" + buffer);
		info.append("]");
		return info.toString();
	}

	@Override
	protected void finalize() throws Throwable {
		free();
	}
	
	/**
	 * 当数据存储到文件时，应用程序读取数据时的内存缓存，
	 * 为了节省内存，里面的数据会重复覆写
	 */
	private class CachedBuffer {
		/** 默认读取缓存，1M */
		public static final int DEFAULT_CACHE_SIZE = 256 * 1024;
		
		/** 内存缓存 */
		private ByteBuffer filebuf = null;
		private long idxStart = 0;
		private long idxEnd = 0;
		
		private int cacheSize = DEFAULT_CACHE_SIZE;
		
		public CachedBuffer() {
			this(DEFAULT_CACHE_SIZE);
		}
		
		public CachedBuffer(int cacheSize) {
			this.cacheSize = cacheSize;
		}
		
		public byte get(long index) throws IOException {
			boolean readFileBuf = filebuf == null || index < idxStart || index > idxEnd;
			if (readFileBuf) {
				idxStart = index;
				// 如果读取的位置在文件里面，将文件里的内容缓存到fileBuf进行读取
				int readLen = (int) (filesize - idxStart > cacheSize ? cacheSize : filesize - idxStart);
				idxEnd = idxStart + readLen - 1;
				if (filebuf == null) {
					filebuf = ByteBuffer.allocate(readLen);
				}
				// 重复复用filebuf内存，除非该内存容量无法容纳更多数据则需要重新创建新的filebuf内存缓存
				filebuf.clear();
				if (filebuf.limit() < readLen) {
					filebuf = null;
					filebuf = ByteBuffer.allocate(readLen);
				}
				readFromFile(filebuf, idxStart);
			}
			int readPos = (int) (index - idxStart);
			filebuf.position(readPos);
			return filebuf.get(readPos);
		}
		
		public void clear() {
			idxStart = 0;
			idxEnd = 0;
			if (filebuf != null) {
				filebuf.clear();
				filebuf = null;
			}
		}
		
		/**
		 * 从文件中读取字节数据
		 * 
		 * @param offset 让缓存文件从哪个位置开始读取数据填入到buf里面
		 */
		private long readFromFile(ByteBuffer buf, long offset) throws IOException {
			if (offset < 0 || buf == null || buf.remaining() < 0) {
				throw new IllegalArgumentException();
			}
			FileChannel fc = getReadfc();
			long readLen = 0;
			long fileRemaining = filesize - offset;
			int bufRemaining = buf.remaining();
			long totalLen = bufRemaining > fileRemaining ? fileRemaining : bufRemaining;
			while (readLen < totalLen) {
				int len = fc.read(buf, offset);
				if (len <= 0) {
					throw new IOException("cache read from file error");
				}
				readLen += len;
				offset += len;
				frcnt++;
			}
			return totalLen;
		}
	}
}
