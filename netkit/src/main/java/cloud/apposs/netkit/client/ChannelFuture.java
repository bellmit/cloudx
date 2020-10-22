package cloud.apposs.netkit.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 异步Channele服务，
 * 主要服务于Client阻塞等待，或者异步监听等
 */
public class ChannelFuture {
	/** 异步请求是否结束，无论请求成功还是失败均会触发结束 */
	private boolean done = false;
	
	/** 等待获取任务执行结果的阻塞锁 */
    private final ReentrantLock donetLock = new ReentrantLock();
    private final Condition hasDone = donetLock.newCondition();
	
	/**
     * 异步请求执行结果监听列表
     */
    private final List<ChannelFutureListener <? extends ChannelFuture>> listeners = 
    	new CopyOnWriteArrayList<ChannelFutureListener <? extends ChannelFuture>>();
    
	public boolean await() throws InterruptedException {
		return await(-1, TimeUnit.MILLISECONDS);
	}

	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		if (isDone()) {
            return true;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        
        final ReentrantLock doneLock = this.donetLock;
        try {
        	doneLock.lockInterruptibly();
			if (timeout > 0) {
				hasDone.await(timeout, unit);
			} else {
				hasDone.await();
			}
		} catch (InterruptedException e) {
			// 线程等待被打断
			hasDone.signal();
		} finally {
			doneLock.unlock();
        }
		
		return isDone();
	}
    
    public void addListener(ChannelFutureListener <? extends ChannelFuture> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener");
		}
		
		listeners.add(listener);
	}

    public void removeListener(ChannelFutureListener <? extends ChannelFuture> listener) {
    	if (listener == null) {
			throw new IllegalArgumentException("listener");
		}
    	
        listeners.remove(listener);
    }

	public boolean isDone() {
		return done;
	}
	
	/**
	 * 任务成功执行结束的回调，主要由线程池内部调用
	 * 
	 * @param result 任务的执行结果
	 */
	public void fireDone() {
		this.done = true;
		final ReentrantLock resultLock = this.donetLock;
		resultLock.lock();
        try {
        	// 通知那些阻塞等待的线程已经有结果了
        	hasDone.signal();
        	// 触发监听服务
        	doNotifyListeners();
        } finally {
        	resultLock.unlock();
        }
	}
	
	@SuppressWarnings("unchecked")
	private void doNotifyListeners() {
    	if (!listeners.isEmpty()) {
    		for (ChannelFutureListener listener : listeners) {
            	listener.channelComplete(this);
            }
    	}
    }
}
