package cloud.apposs.threadx;

/**
 * 线程池监听器
 */
public interface ThreadServiceListener {
	/**
	 * 线程池调用{@link ThreadPool#shutdown()}时的监听
	 * 
	 * @param service 线程池
	 */
	void serviceShutdown(ThreadService service);
	
	/**
	 * 线程池调用{@link ThreadPool#shutdown()}并且所有线程已经执行完毕时的监听
	 * 
	 * @param service 线程池
	 */
	void serviceTerminated(ThreadService service);
	
	/**
	 * 线池资源已耗竭时的监听，即此时线程池无法再自动扩展工作线程
	 * 
	 * @param service 线程池
	 */
	void serviceExhausted(ThreadService service);
	
	/**
	 * 任务执行前的监听
	 * 
	 * @param worker 线程池中的工作线程
	 * @param task 要执行的任务
	 */
	void beforeThreadProcess(Thread worker, Processor task);
	
	/**
	 * 任务执行前的监听
	 * 
	 * @param worker 线程池中的工作线程
	 * @param task 要执行的任务
	 */
	void beforeThreadExecute(Thread worker, Runnable task);
	
	/**
	 * 任务执行后的监听
	 * 
	 * @param worker 线程池中的工作线程
	 * @param task 要执行的任务
	 */
	void afterThreadProcess(Thread worker, Processor task, Throwable cause);
	
	/**
	 * 任务执行后的监听
	 * 
	 * @param worker 线程池中的工作线程
	 * @param task 要执行的任务
	 */
	void afterThreadExecute(Thread worker, Runnable task, Throwable cause);
}
