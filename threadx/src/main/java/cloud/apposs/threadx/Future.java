package cloud.apposs.threadx;

import java.util.concurrent.TimeUnit;

public interface Future<V> extends java.util.concurrent.Future<V> {
	/**
	 * 一直阻塞等待任务执行结果
	 * 
	 * @return 等待成功返回true
	 */
	boolean await() throws InterruptedException;

	/**
	 * 阻塞等待任务执行结果，直到到达指定超时时间
	 * 
	 * @param  timeoutMillis 等待超时时间，单位为毫秒，小于0为一直等待
	 * @return 等待成功返回true
	 */
	boolean await(long timeoutMillis) throws InterruptedException;

	/**
	 * 阻塞等待任务执行结果，直到到达指定超时时间
	 * 
	 * @param  timeout 等待超时时间，小于0为一直等待
	 * @param  unit    等待时间单位
	 * @return 等待成功返回true
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * 不阻塞等待，立即返回结果，无论任务有没有执行结束
	 * 
	 * @return 任务的执行结果
	 */
	V getNow();
	
	/**
	 * 获取任务执行可能出现的异常
	 */
	Throwable cause();

	/**
	 * 添加异步回执模型的监听
	 */
	void addListener(FutureListener<? extends Future<?>> listener);

	/**
	 * 移除异步回执模型的监听
	 */
	void removeListener(FutureListener<? extends Future<?>> listener);
}
