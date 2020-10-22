package cloud.apposs.threadx;

import java.util.EventListener;

/**
 * 异步任务执行监听服务
 */
public interface FutureListener <F extends Future<?>> extends EventListener {
	/**
	 * 任务执行成功的监听
	 * 
	 * @param future 异步任务模型
	 * @param cause 如果任务执行时产生了异常，则该值不为空
	 */
	void executeComplete(F future, Throwable cause);
}
