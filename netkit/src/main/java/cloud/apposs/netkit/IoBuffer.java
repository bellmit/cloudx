package cloud.apposs.netkit;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public interface IoBuffer {
	public static final int EOF = -1;

	long readIdx();
	long writeIdx();
	IoBuffer readIdx(long readIdx);
	IoBuffer writeIdx(long writeIdx);

	long readableBytes();
	long writableBytes();
	boolean hasReadableBytes();
	boolean hasWritableBytes();
	
	long capacity();
	
	/**
	 * 获取字节，获取数据后{@link #readIdx()}会递增
	 */
	byte get() throws IOException;
	
	/**
	 * 获取指定位置字节，获取数据后{@link #readIdx()}不会递增
	 */
	byte get(long index) throws IOException;
	/**
	 * 获取字节数组，将获取的字节添加到dst中，
	 * 规则：获取数组列表之后{@link #readIdx()}会同步更新
	 */
	public IoBuffer get(byte[] dst) throws IOException;
	public IoBuffer get(byte[] dst, long offset, int length) throws IOException;
	/**
	 * 获取字节内容，
	 * 规则：该方法下所有的字节默认获取均是以{@link #readIdx()}开始，
	 * {@link #writeIdx() - #readIdx()}结束，并且readIdx指标不会位移
	 */
	byte[] array() throws IOException;
	byte[] array(long offset, int length) throws IOException;
	String string();
	String string(Charset charset);
	String string(long offset, int length);
	String string(long offset, int length, Charset charset);
	/***
	 * 返回的ByteBuffer在数据读取之后position会位移，不做重置，
	 * 所以如果要读取ByteBuffer的数据需要调用{@link ByteBuffer#flip()}
	 */
	ByteBuffer buffer() throws IOException;
	ByteBuffer buffer(long offset, int length) throws IOException;
	ByteBuffer buffer(long offset, int length, boolean direct) throws IOException;
	
	/**
	 * 添加数据，添加数据后{@link #writeIdx()}需要更新状态
	 */
	IoBuffer put(byte b) throws IOException;
	IoBuffer put(byte[] src) throws IOException;
	IoBuffer put(byte[] src, int offset, int length) throws IOException;
	
	/**
	 * 添加ByteBuffer数据，添加数据后{@link #writeIdx()}需要更新状态，
	 * 从{@link ByteBuffer#position()}开始到{@link ByteBuffer#limit()}结束
	 * 
	 * @param src，规则：添加数据前后position会递增到(offset + length)
	 */
	IoBuffer put(ByteBuffer src) throws IOException;
	IoBuffer put(ByteBuffer src, int offset, int length) throws IOException;
	
	/**
	 * 添加ByteBuffer数据，添加数据后{@link #writeIdx()}需要更新状态，
	 * 从{@link IoBuffer#writeIdx()}开始到{@link IoBuffer#readIdx()}结束，
	 * 
	 * @param src，规则：src添加数据后readIdx会递增到(offset + length)
	 */
	IoBuffer put(IoBuffer src) throws IOException;
	IoBuffer put(IoBuffer src, long offset, long length) throws IOException;
	
	IoBuffer put(String src) throws IOException;
	IoBuffer put(String src, Charset charset) throws IOException;
	
	/**
	 * 添加文件内容，会将src文件内容直接拷贝到当前文件内存缓存中
	 */
	IoBuffer put(File src) throws IOException;
	IoBuffer put(File src, long offset, long length) throws IOException;

	IoBuffer put(FileChannel src) throws IOException;
	IoBuffer put(FileChannel src, long offset, long length) throws IOException;
	
	/**
	 * 往管道读取数据，数据读取后{@link #writeIdx()}需要更新状态
	 * 
	 * @return 读取多少字节
	 */
	long channelRecv(EventChannel channel) throws IOException;
	
	/**
	 * 往管道发送数据，数据发送后{@link #readIdx()}需要更新状态
	 * 
	 * @return 发送多少字节
	 */
	long channelSend(EventChannel channel) throws IOException;
	
	/**
	 * 将内存数据刷到磁盘
	 */
	long flush(String file) throws IOException;
	long flush(File file) throws IOException;

	/**
	 * Buffer手动扩容
	 */
	void expand(int length) throws IOException;
	
	/**
	 * 重置内存状态，在业务处理中重复利用该特性进行重复读写而不是每次网络数据处理都需要创建内存
	 */
	void reset();
	
	/**
	 * 整个会话关闭的资源释放，如果该内存是在内存池中分配则需要在使用完时释放内存空间，
	 * 要支持重复调用，并保证第二次调用时不需要再做资源释放操作
	 */
	void free();
}
