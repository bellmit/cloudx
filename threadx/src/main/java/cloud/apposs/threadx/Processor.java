package cloud.apposs.threadx;

import cloud.apposs.threadx.ThreadService.ThreadContext;

/**
 * 任务，用于执行用户自定义的操作
 */
public interface Processor {
	/**
	 * 执行任何所需的操作，操作完成时此任务自动回收到线程池中
	 * 
	 * @param context 线程池上下文，注意此上下文为全线程池共享，所有线程对其的修改在全线程池可见
	 */
	void process(ThreadContext context);
}
