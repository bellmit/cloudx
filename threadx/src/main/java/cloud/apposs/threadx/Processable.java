package cloud.apposs.threadx;

/**
 * 任务，用于执行用户自定义的操作，
 * 相对于Runnable接口，该任务接口能够支持线程池上下文和完成/异常监听
 */
public interface Processable<V> {
	/**
	 * 执行任何所需的操作，操作完成时此任务自动回收到线程池中
	 * 
	 * @param  context 线程池上下文，注意此上下文为全线程池共享，所有线程对其的修改在全线程池可见
	 * @return 任务执行结果
	 */
	V process(ThreadContext context) throws Exception;
}
