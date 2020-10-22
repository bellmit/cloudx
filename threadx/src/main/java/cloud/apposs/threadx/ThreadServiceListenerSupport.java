package cloud.apposs.threadx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 线程池监听器辅助类，负责对所有{@link ThreadServiceListener}的添加、移除和事件触发
 */
public class ThreadServiceListenerSupport {
	private final ThreadService service;
	
	/** 所有的监听器 */
	private final List<ThreadServiceListener> listeners = 
		new CopyOnWriteArrayList<ThreadServiceListener>();
	
	public ThreadServiceListenerSupport(ThreadService service) {
        if (service == null) {
            throw new NullPointerException();
        }
        this.service = service;
    }
	
	/**
     * 添加监听器
     * 
     * @param listener 监听器
     */
    public void add(ThreadServiceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * 移除监听器
     * 
     * @param listener 监听器
     */
    public void remove(ThreadServiceListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#serviceShutdown(ThreadService)}事件
     */
    public void fireServiceShutdown() {
        for (ThreadServiceListener listener : listeners) {
            listener.serviceShutdown(service);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#serviceTerminated(ThreadService)}事件
     */
    public void fireServiceTerminated() {
        for (ThreadServiceListener listener : listeners) {
            listener.serviceTerminated(service);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#serviceExhausted(ThreadService)}事件
     */
    public void fireServiceExhausted() {
        for (ThreadServiceListener listener : listeners) {
            listener.serviceExhausted(service);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#beforeThreadProcess(Thread, Processor)}事件
     */
    public void fireBeforeThreadProcess(Thread worker, Processor task) {
        for (ThreadServiceListener listener : listeners) {
            listener.beforeThreadProcess(worker, task);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#beforeThreadExecute(Thread, Runnable)}事件
     */
    public void fireBeforeThreadExecute(Thread worker, Runnable task) {
        for (ThreadServiceListener listener : listeners) {
            listener.beforeThreadExecute(worker, task);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#afterThreadProcess(Thread, Processor, Throwable)}事件
     */
    public void fireAfterThreadProcess(Thread worker, Processor task, Throwable t) {
        for (ThreadServiceListener listener : listeners) {
            listener.afterThreadProcess(worker, task, t);
        }
    }
    
    /**
     * 触发所有{@link ThreadServiceListener#afterThreadExecute(Thread, Runnable, Throwable)}事件
     */
    public void fireAfterThreadExecute(Thread worker, Runnable task, Throwable t) {
        for (ThreadServiceListener listener : listeners) {
            listener.afterThreadExecute(worker, task, t);
        }
    }
}
