package cloud.apposs.threadx;

/**
 * ThreadPool JMX
 */
public interface ThreadPoolMBean {
	/**
	 * 返回核心线程数
	 * 
	 * @return 核心线程数
	 */
	int getCorePoolSize();
	
	/**
	 * 返回最大线程数
	 * 
	 * @return 核心线程数
	 */
	int getMaxPoolSize();
	
	/**
	 * 返回当前空闲线程数
	 * 
	 * @return 当前空闲线程数
	 */
	int getNumIdle();
	
	/**
	 * 返回忙碌线程数
	 * 
	 * @return 忙碌线程数
	 */
	int getNumBusy();
	
	/**
	 * 判断当前线程池是否资源耗竭
	 * 
	 * @return
	 */
	boolean isExhausted();
	
	/**
	 * 判断当前线程池是否已经关闭
	 * 
	 * @return
	 */
	boolean isShutdown();
	
	/**
	 * 判断当前线程池是否完全关闭
	 * 
	 * @return
	 */
	boolean isTerminated();
}
