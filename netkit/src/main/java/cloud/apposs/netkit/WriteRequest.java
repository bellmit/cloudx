package cloud.apposs.netkit;

import cloud.apposs.netkit.buffer.ByteBuf;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class WriteRequest {
	public static final IoBuffer CLOSE_REQUEST = new ByteBuf(0, false);
	
	private long totalSendBytes = 0;
	
	private final Queue<IoBuffer> writeQueue = new ConcurrentLinkedQueue<IoBuffer>();
	
	/**
	 * 要发送的原始数据，因为原始数据经过过滤器过滤之后可能数据不再有意义，
	 * 存储此数据列表用于当会话处理结束时释放资源
	 */
	private final Queue<IoBuffer> rawQueue = new ConcurrentLinkedQueue<IoBuffer>();
	
	private IoBuffer currentWriteRequest;
	
	private IoBuffer lastWriteRequest;
	
	public final long getTotalSendBytes() {
		return totalSendBytes;
	}
	
	public final IoBuffer getCurrentWriteMessage() {
        return currentWriteRequest;
    }

    public final void setCurrentWriteRequest(IoBuffer currentWriteRequest) {
        this.currentWriteRequest = currentWriteRequest;
    }
    
    public final IoBuffer getLastWriteMessage() {
        return lastWriteRequest;
    }

    public final void setLastWriteRequest(IoBuffer lastWriteRequest) {
        this.lastWriteRequest = lastWriteRequest;
    }

	public final synchronized void offer(IoBuffer buffer) {
		totalSendBytes += buffer.readableBytes();
		writeQueue.offer(buffer);
	}
	
	protected final synchronized void addRawWriteRequest(IoBuffer buffer) {
		rawQueue.offer(buffer);
	}
	
	public final synchronized IoBuffer poll() {
		return writeQueue.poll();
	}
	
	public final synchronized boolean isEmpty() {
		return writeQueue.isEmpty();
	}
	
	public final synchronized void clear() {
		totalSendBytes = 0;
		currentWriteRequest = null;
		lastWriteRequest = null;
		for (IoBuffer buffer = writeQueue.poll(); buffer != null; buffer = writeQueue.poll()) {
			buffer.free();
		}
		for (IoBuffer buffer = rawQueue.poll(); buffer != null; buffer = rawQueue.poll()) {
			buffer.free();
		}
	}
}
