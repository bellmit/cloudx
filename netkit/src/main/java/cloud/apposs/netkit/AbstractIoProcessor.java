package cloud.apposs.netkit;

import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterChain;
import cloud.apposs.netkit.listener.IoListenerSupport;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractIoProcessor implements IoProcessor {
	public static final int DEFAULT_CONNECT_TIMEOUT = 5 * 1000;
	public static final int DEFAULT_SEND_TIMEOUT = 60 * 1000;
	public static final int DEFAULT_RECV_TIMEOUT = 60 * 1000;
	
	public static final int DEFAULT_BUFFER_SIZE = 2 * 1024;
	public static final boolean DEFAULT_BUFFER_DIRECT = true;
	public static final int DEFAULT_MAX_READ_BUFFER_SIZE = 65536;
	
	protected final IoEvent event;
	
	protected SelectionKey key;
	
	protected Object context;
	
	protected IoFilterChain chain;
	
	private IoListenerSupport listenerSupport;
	
	private long flow;
	
	/** 当前会话请求存储的一些状态值 */
	private final Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>(1);
	
	/** 发送队列 */
	protected final WriteRequest writeRequest = new WriteRequest();

	/** 首次建立会话的时间 */
	private long createTime;
	/** 处理数据包的时间，数据接收和发送时会更新当前时间，主要用于配置处理会话读写超时 */
	private long actionTime = 0;

	/** 
	 * 请求时间相关配置 
	 */
	/** 网络连接超时时间 */
	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	/** 网络发送超时时间 */
	private int sendTimeout = DEFAULT_SEND_TIMEOUT;
	/** 网络接收超时时间 */
	private int recvTimeout = DEFAULT_RECV_TIMEOUT;
	
	/**
	 * 接收/发送内存缓存相关配置
	 */
	/** 数据接收/发送默认分配缓存大小，默认256K */
	private int bufferSize = DEFAULT_BUFFER_SIZE;
	/** 数据接收/发送是否采用堆外缓存，默认为采用，网络IO用堆外缓存性能更优些 */
	private boolean bufferDirect = DEFAULT_BUFFER_DIRECT;
	private int maxBufferSize = DEFAULT_MAX_READ_BUFFER_SIZE;
	
	public AbstractIoProcessor() {
		this(SysUtil.random());
	}
	
	public AbstractIoProcessor(int flow) {
		this.chain = new IoFilterChain(this);
		this.event = new IoEvent();
		this.flow = flow;
		this.createTime = System.currentTimeMillis();
		this.actionTime = System.currentTimeMillis();
	}

	@Override
	public IoEvent getEvent() {
		return event;
	}

	@Override
	public long getFlow() {
		return flow;
	}
	
	@Override
	public void setFlow(long flow) {
		this.flow = flow;
	}

	@Override
	public Object getContext() {
		return context;
	}

	@Override
	public void setContext(Object context) {
		this.context = context;
	}

	@Override
	public IoFilterChain getFilterChain() {
		return chain;
	}

	@Override
	public void setFilterChain(IoFilterChain chain) {
		this.chain = chain;
	}
	
	@Override
	public long getCreateTime() {
		return createTime;
	}

	@Override
	public long getActionTime() {
		return actionTime;
	}

	@Override
	public void setActionTime(long actionTime) {
		this.actionTime = actionTime;
	}

	@Override
	public int getConnectTimeout() {
		return connectTimeout;
	}

	@Override
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	@Override
	public int getSendTimeout() {
		return sendTimeout;
	}

	@Override
	public void setSendTimeout(int sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	public int getRecvTimeout() {
		return recvTimeout;
	}

	@Override
	public void setRecvTimeout(int recvTimeout) {
		this.recvTimeout = recvTimeout;
	}


	@Override
	public int getBufferSize() {
		return bufferSize;
	}
	
	@Override
	public void setBufferSize(int bufferSize) {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException("bufferSize");
		}
		this.bufferSize = bufferSize;
	}

	@Override
	public boolean isBufferDirect() {
		return bufferDirect;
	}
	
	@Override
	public void setBufferDirect(boolean bufferDirect) {
		this.bufferDirect = bufferDirect;
	}

	@Override
	public int getMaxBufferSize() {
		return maxBufferSize;
	}

	@Override
	public void setMaxBufferSize(int maxBufferSize) {
		if (maxBufferSize <= 0) {
			throw new IllegalArgumentException("maxBufferSize");
		}
		this.maxBufferSize = maxBufferSize;
	}

	@Override
	public final long write(byte[] buf) throws IOException {
		return write(IoAllocator.wrap(buf));
	}
	
	@Override
	public final long write(String str) throws IOException {
		return write(str, Charset.forName("utf-8"));
	}

	@Override
	public final long write(String str, Charset charset) throws IOException {
		SysUtil.checkNotNull(str, "str");
		byte[] buf = str.getBytes(charset);
		return write(IoAllocator.wrap(buf));
	}
	
	@Override
	public long write(ByteBuffer buf) throws IOException {
		return write(buf, true);
	}

	@Override
	public long write(ByteBuffer buf, boolean flip) throws IOException {
		SysUtil.checkNotNull(buf, "buf");
		if (flip) buf.flip();
		return write(IoAllocator.wrap(buf));
	}

	@Override
	public long write(IoBuffer buffer) throws IOException {
		SysUtil.checkNotNull(buffer, "buffer");
		
		long len = buffer.readableBytes();
		try {
			doFireFilterWrite(buffer);
			// 转换写事件触发EventLoop.doSend(SelectionKey)发送数据
			event.setEvent(IoEvent.OP_WRITE);
		} catch (Exception e) {
			throw new IOException(e);
		}
		return len;
	}
	
	@Override
	public long write(List<IoBuffer> buffers) throws IOException {
		SysUtil.checkNotNull(buffers, "buffers");
		long len = 0;
		try {
			for (IoBuffer buffer : buffers) {
				len += buffer.readableBytes();
				doFireFilterWrite(buffer);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
		// 转换写事件触发EventLoop.doSend(SelectionKey)发送数据
		event.setEvent(IoEvent.OP_WRITE);
		return len;
	}

	@Override
	public long write(IoBuffer... buffers) throws IOException {
		SysUtil.checkNotNull(buffers, "buffers");
		long len = 0;
		try {
			for (int i = 0; i < buffers.length; i++) {
				IoBuffer buffer = buffers[i];
				doFireFilterWrite(buffer);
			}
		} catch(Exception e) {
			throw new IOException(e);
		}
		// 转换写事件触发EventLoop.doSend(SelectionKey)发送数据
		event.setEvent(IoEvent.OP_WRITE);
		return len;
	}

	@Override
	public void flush() {
		// 如果有数据要发送，注册发送事件并唤醒EventLoop
		if (!writeRequest.isEmpty()) {
			IoEvent.registSelectionKeyEvent(event, key, IoEvent.OP_WRITE);
		}
	}

	private void doFireFilterWrite(IoBuffer buffer) throws Exception {
		try {
			chain.fireFilterWrite(buffer);
		} finally {
			if (writeRequest.getLastWriteMessage() == buffer) {
				// 原始数据未被FilterWrite过滤，需要添加原始数据以便于底层释放资源
				// 如果原始数据已被IoFilter过滤则则IoFilter是需要对重新编码的数据做资源释放
				writeRequest.addRawWriteRequest(buffer);
				writeRequest.setLastWriteRequest(null);
			}
		}
	}

	@Override
	public final WriteRequest getWriteRequest() {
		if (writeRequest == null) {
            throw new IllegalStateException();
        }
		return writeRequest;
	}

	@Override
	public final IoListenerSupport getListenerSupport() {
		return listenerSupport;
	}
	
	@Override
	public final void setListenerSupport(IoListenerSupport listenerSupport) {
		this.listenerSupport = listenerSupport;
	}

	@Override
	public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }
	
	@Override
	public final Object getAttribute(Object key, Object defaultVal) {
        Object attr = attributes.get(key);
        if (attr == null && defaultVal != null) {
        	attr = defaultVal;
        	attributes.put(key, attr);
        }
        return attr;
    }
	
	@Override
	public final Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }
	
	@Override
	public final boolean hasAttribute(Object key) {
        return attributes.containsKey(key);
    }

	@Override
	public final SelectionKey register(Selector selector) throws IOException {
		setActionTime(System.currentTimeMillis());
		final SelectionKey key = doRegister(selector);
		this.key = key;
		return key;
	}
	
	public abstract SelectionKey doRegister(Selector selector) throws IOException;
	
	@Override
	public SelectionKey selectionKey() {
		return key;
	}

	@Override
	public void channelAccept(EventChannel channel) throws Exception {
	}
	
	@Override
	public void channelConnect() throws Exception {
	}
	
	@Override
	public void channelRead(Object message) throws Exception {
	}
	
	@Override
	public void channelReadEof(Object message) throws Exception {
	}

	@Override
	public void channelSend(WriteRequest request) throws Exception {
	}
	
	@Override
	public void channelClose() {
		try {
			if (key != null) {
				key.channel().close();
				key.cancel();
				key.attach(null);
				key = null;
			}
    	} catch(Throwable t) {
    	}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void channelError(Throwable cause) {
		// 该代码主要服务响应式异步调用
		if (context instanceof IoSubscriber) {
			IoSubscriber subscribe = (IoSubscriber) getContext();
			subscribe.onError(cause);
		}
	}
	
	@Override
	public void close(boolean immediately) {
		if (immediately) {
			IoEvent.registSelectionKeyEvent(event, key, IoEvent.OP_CLOSE);
		} else {
			writeRequest.offer(WriteRequest.CLOSE_REQUEST);
			IoEvent.registSelectionKeyEvent(event, key, IoEvent.OP_CLOSING);
		}
	}

	@Override
	public void clear() {
	}
}
