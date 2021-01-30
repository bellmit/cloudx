package cloud.apposs.threadx;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池异步任务模型，
 * 支持阻塞等待执行结果，或者以添加监听回调的方式获取任务执行结果
 */
public class FutureTask<V> implements Future<V>, Processor, Runnable {
	/** 业务实现的任务接口，可以实现以下任一接口 */
	private final Callable<V> callable;
	private final Processable<V> processable;
	
	/** 任务执行可能出现的异常 */
	private Throwable cause;
	
	/** 任务执行结果 */
	private V result;
	
	/***
	 * 任务是否已经被取消，即调用了{@link #cancel(boolean)}
	 */
	private boolean cancelled = false;
	
	/** 等待获取任务执行结果的阻塞锁 */
    private final ReentrantLock resultLock = new ReentrantLock();
    private final Condition hasResult = resultLock.newCondition();
    
    /**
     * 异步任务执行结果监听列表
     */
    private final List<FutureListener<? extends Future<?>>> listeners = 
    	new CopyOnWriteArrayList<FutureListener<? extends Future<?>>>();
	
	public FutureTask(Processable<V> processable) {
		if (processable == null) {
			throw new IllegalArgumentException("processable");
		}
		
		this.callable = null;
		this.processable = processable;
	}
	
	public FutureTask(Callable<V> callable) {
		if (callable == null) {
			throw new IllegalArgumentException("callable");
		}
		
		this.callable = callable;
		this.processable = null;
	}
	
	@Override
	public void run() {
		V result = null;
		try {
			result = callable.call();
		} catch (Throwable cause) {
			cause(cause);
		} finally {
			done(result);
		}
	}
	
	@Override
	public void process(ThreadContext context) {
		V result = null;
		try {
			result = processable.process(context);
		} catch (Throwable cause) {
			cause(cause);
		} finally {
			done(result);
		}
	}

	@Override
	public Throwable cause() {
		return cause;
	}

	@Override
	public boolean await() throws InterruptedException {
		return await(-1, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		if (isDone()) {
            return true;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        
        final ReentrantLock resultLock = this.resultLock;
        try {
        	resultLock.lockInterruptibly();
			if (timeout > 0) {
				hasResult.await(timeout, unit);
			} else {
				hasResult.await();
			}
		} catch (InterruptedException e) {
			// 线程等待被打断
			hasResult.signal();
		} finally {
			resultLock.unlock();
        }
		
		return isDone();
	}
	
	/**
	 * 判断任务是否执行结束
	 */
	@Override
	public boolean isDone() {
		return result != null;
	}

	@Override
	public V getNow() {
		return result;
	}

	/**
	 * 一直阻塞获取任务执行结果
	 * 
	 * @throws ExecutionException 执行的过程中可能出现了异常则抛出此异常
	 */
	@Override
	public V get() throws InterruptedException, ExecutionException {
		if (await()) {
	        Throwable cause = cause();
	        if (cause == null) {
	            final V result = getNow();
	            this.result = null;
	            return result;
	        }
		}
        // 任务执行的过程出现了异常
        throw new ExecutionException(cause);
	}

	/**
	 * 阻塞获取任务执行结果直到超时
	 * 
	 * @throws ExecutionException 执行的过程中可能出现了异常则抛出此异常
	 * @throws TimeoutException 等待超时的异常
	 */
	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		if (await(timeout, unit)) {
            Throwable cause = cause();
            if (cause == null) {
            	final V result = getNow();
                this.result = null;
                return result;
            }
            throw new ExecutionException(cause);
        }
        throw new TimeoutException();
	}
	
	/**
	 * 取消任务的阻塞等待，
	 * 即另外一些线路可能在执行{@link #await()}阻塞等待结果
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		final ReentrantLock resultLock = this.resultLock;
		resultLock.lock();
        try {
        	hasResult.signal();
        } finally {
        	resultLock.unlock();
        }
        return true;
	}

	/**
	 * 判断任务是否已经被取消，即调用了{@link #cancel(boolean)}
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void addListener(FutureListener<? extends Future<?>> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener");
		}
		
		listeners.add(listener);
		// 可能在添加的过程中任务已经执行完毕，直接触发监听
		if (isDone()) {
            doNotifyListeners();
        }
	}

	@Override
    public void removeListener(FutureListener<? extends Future<?>> listener) {
    	if (listener == null) {
			throw new IllegalArgumentException("listener");
		}
    	
        listeners.remove(listener);
    }
    
    /**
	 * 任务执行异常的回调，主要由线程池内部调用
	 */
	private void cause(Throwable cause) {
		this.cause = cause;
	}

	/**
	 * 任务成功执行结束的回调，主要由线程池内部调用
	 * 
	 * @param result 任务的执行结果
	 */
	private void done(V result) {
		this.result = result;
		final ReentrantLock resultLock = this.resultLock;
		resultLock.lock();
        try {
        	// 通知那些阻塞等待的线程已经有结果了
        	hasResult.signal();
        	// 触发监听服务
        	doNotifyListeners();
        } finally {
        	resultLock.unlock();
        }
	}
    
    @SuppressWarnings("unchecked")
	private void doNotifyListeners() {
    	if (!listeners.isEmpty()) {
    		for (FutureListener listener : listeners) {
            	listener.executeComplete(this, cause);
            }
    	}
    }
}
