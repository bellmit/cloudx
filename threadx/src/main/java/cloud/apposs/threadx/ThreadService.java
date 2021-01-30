package cloud.apposs.threadx;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * 线程池接口，用于管理所有线程
 */
public interface ThreadService extends Executor {
	/**
	 * 获取一个空闲任务用于执行
	 * 注意！此方法在线程池为空且到达最大线程数时会阻塞等待
	 * 直到有新的任务添加到线程池或者任务已执行完成并回收到线程中
	 * 
	 * @return 线程池中空闲任务
	 */
	Task getIdleTask();
	
	/**
	 * 获取一个空闲任务用于执行，如果此队列中没有任何元素，则等待指定等待的时间（如果有必要）
	 * 
	 * @param waitTime 等待超时时间，以毫秒为单位，小于0时为阻塞等待直到有新的任务添加到线程池或者任务已执行完成并回收到线程中
	 * @return 线程池中空闲任务
	 */
	Task getIdleTask(long waitTime);
	
	/**
	 * 获取一个空闲任务用于执行
	 * 如果线程池已经没有空闲线程则直接返回Null
	 * 
	 * @return 线程池中空闲任务
	 */
	Task getIdleTaskNow();
	
	/**
	 * 从线程池中取出空闲线程执行用户定义处理器
	 * 注意！此方法会调用{@link #getIdleTask()}获取空闲线程
	 * 所以在线程池为空且到达最大线程数时会阻塞等待
	 * 直到有新的任务添加到线程池或者任务已执行完成并回收到线程中
	 * 形式等同:{getIdleTask().addProcessor(Processor)}的简化版
	 * 
	 * @param processor 处理器
	 */
	void execute(Processor processor);
	
	/**
	 * 从线程池中取出空闲线程执行用户定义任务
	 * 注意！此方法会调用{@link #getIdleTask()}获取空闲线程
	 * 所以在线程池为空且到达最大线程数时会阻塞等待
	 * 直到有新的任务添加到线程池或者任务已执行完成并回收到线程中
	 * 形式等同:{getIdleTask().addExecutor(Runnable)}的简化版
	 * 
	 * @param runnable 要提交的任务
	 */
	void execute(Runnable runnable);
	
	/**
	 * 执行结果任务，任务的执行结果将会以异步模型返回
	 */
	<V> FutureTask<V> submit(Callable<V> callable);
	
	/**
	 * 执行结果任务，任务的执行结果将会以异步模型返回
	 */
	<V> FutureTask<V> submit(Processable<V> processable);
	
	/**
	 * 判断线程池线程资源已经耗竭
	 * 耗竭原因为空闲线程为0且线程池已经到达最大线程数
	 * 处理耗竭状态的线程无法再添加线程且在获取空闲线程时为阻塞状态，除非有任务执行完成自动回收到线程池中
	 * 
	 * @return 已经耗竭返回true
	 */
	boolean isExhausted();
	
	/**
	 * 获取线程池中空闲线程数
	 * 
	 * @return 线程池中空闲线程数
	 */
	int getNumIdle();
	
	/**
	 * 获取线程池中忙碌线程数
	 * 
	 * @return 线程池中忙碌线程数
	 */
	int getNumBusy();
	
	/**
	 * 关闭线程池，
	 * 关闭不会中断正在执行的任务
	 */
	void shutdown();
	
	/**
	 * 关闭线程池，
	 * 同时也会中断正在执行的任务
	 */
	void shutdownNow();
	
	/**
	 * 关闭线程池，如果已经关闭，则调用没有其他作用
	 * 
	 * @param interrupt 是否中断目前正在执行的任务
	 */
	void shutdown(boolean interrupt);
	
	/**
     * 判断线程池是否已经关闭
     *
     * @return 如果线程池已关闭，则返回<code>true</code>
     */
	boolean isShutdown();

    /**
     * 如果关闭后所有任务都已完成，则返回 true。
     * 注意，除非首先调用 shutdown，否则isTerminated永不为 true。 
     *
     * @return 如果关闭后所有任务都已完成，则返回<code>true</code>
     */
    boolean isTerminated();
    
    /**
     * 等待线程池完全关闭
     * 在请求关闭、发生超时或者当前线程中断这此操作发生之前，此方法都一直阻塞，直到所有任务完成执行。 
     * 
     * @param  timeout 最长等待时间，以毫秒为单位
     * @return 如果此执行程序终止，则返回 true；如果终止前超时期满，则返回 false
     * @throws InterruptedException 如果等待时发生中断
     */
    boolean awaitTermination(long timeout) throws InterruptedException;
    
    /**
     * 等待线程池完全关闭
     * 在请求关闭或者当前线程中断这此操作发生之前，此方法都一直阻塞，直到所有任务完成执行。
     * 
     * @throws InterruptedException 如果等待时发生中断
     */
    void awaitTermination() throws InterruptedException;
    
    /**
	 * 添加线程池监听器
	 * 
	 * @param listener 监听器
	 */
	void addListener(ThreadServiceListener listener);
	
	/**
     * 移除线程池监听器
	 * 
	 * @param listener 监听器
     */
    void removeListener(ThreadServiceListener listener);
    
    /**
     * 线程任务
     */
    interface Task extends Runnable {
    	/**
    	 * 添加用户定义处理器，用于任务执行
    	 * 
    	 * @param processor 要提交的处理器
    	 */
    	void addProcessor(Processor processor);
    	
    	/**
    	 * 添加用户定义任务，用于任务执行
    	 * 
    	 * @param runnable 要提交的任务
    	 */
    	void addExecutor(Runnable runnable);
    	
    	/**
		 * 验证任务的有效性
		 * 此方法在线程池加入或者取出任务时将会调用验证
		 * 验证不通过无法从线程池中加入或者取出任务并执行{@link #destroy(boolean)}操作
		 * 
		 * @return 验证通过返回true
		 */
		boolean validate();
		
		/**
		 * 激活任务，在线程池取出任务时调用
		 */
		void active();
		
		/**
		 * 钝化任务，在添加或回收任务到线程池时调用
		 */
		void passive();
		
		/**
		 * 销毁任务，在任务验证失败或者线程池关闭时调用
		 * 
		 * @param interrupt 是否中断目前正在执行的任务
		 */
		void destroy(boolean interrupt);
    }
    
    /**
     * 线程池上下文，所有资源全线程池共享
     */
    interface IThreadContext {
    	/**
    	 * 获取线程池
    	 * 
    	 * @return 线程池
    	 */
    	ThreadService getPool();
    	
    	/**
         * 获取键值
         *
         * @param  key 键
         * @return 不存在键对应的值则返回<tt>null</tt>
         */
        Object getAttribute(Object key);

        /**
         * 获取键值，如果不存在该键对应的值则返回<code>defaultValue</code>
         * <pre>
         * if (containsAttribute(key)) {
         *     return getAttribute(key);
         * } else {
         *     setAttribute(key, defaultValue);
         *     return defaultValue;
         * }
         * </pre>
         */
        Object getAttribute(Object key, Object defaultValue);

        /**
         * 自定义键值
         *
         * @param key   键
         * @param value 值
         * @return 如果存在原键对应的值，返回旧的值
         */
        Object setAttribute(Object key, Object value);

        /**
         * 自定义键，用于属性标识
         *
         * @param  key 键
         * @return 如果存在原键对应的值，返回旧的值
         */
        Object setAttribute(Object key);

        /**
         * 设置键值
         * <pre>
         * if (containsAttribute(key)) {
         *     return getAttribute(key);
         * } else {
         *     return setAttribute(key, value);
         * }
         * </pre>
         */
        Object setAttributeIfAbsent(Object key, Object value);

        /**
         * 设置键值
         * <pre>
         * if (containsAttribute(key)) {
         *     return getAttribute(key);  // might not always be Boolean.TRUE.
         * } else {
         *     return setAttribute(key);
         * }
         * </pre>
         */
        Object setAttributeIfAbsent(Object key);

        /**
         * 移除键值
         */
        Object removeAttribute(Object key);

        /**
         * 移除键值
         * <pre>
         * if (containsAttribute(key) && getAttribute(key).equals(value)) {
         *     removeAttribute(key);
         *     return true;
         * } else {
         *     return false;
         * }
         * </pre>
         */
        boolean removeAttribute(Object key, Object value);

        /**
         * 替换键值
         * <pre>
         * if (containsAttribute(key) && getAttribute(key).equals(oldValue)) {
         *     setAttribute(key, newValue);
         *     return true;
         * } else {
         *     return false;
         * }
         * </pre>
         */
        boolean replaceAttribute(Object key, Object oldValue, Object newValue);

        /**
         * 是否包含指定键值
         */
        boolean containsAttribute(Object key);

        /**
         * 获取自定义键集
         */
        Set<Object> getAttributeKeys();
    }
    
    /**
     * 当线程池不拒绝做任何操作时抛出的异常，拒绝的原因有：
     * 		1、线程池已经关闭
     */
    interface RejectedExecutionHandler {
    	void rejectedExecution(Task task, ThreadService pool);
    }
}
