package cloud.apposs.netkit.filterchain.executor;

public interface Task extends Runnable {
	/** 当前任务执行的流水号 */
	long getFlow();
	
	/**
	 * 执行线程任务组，同一个组内的任务执行数受{@link #getLimit()}}影响
	 */
	String getGroup();
	
	/**
	 * 同一个线程组内最多可以执行多少任务数
	 */
	int getLimit();
}
