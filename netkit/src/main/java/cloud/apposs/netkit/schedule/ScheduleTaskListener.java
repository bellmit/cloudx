package cloud.apposs.netkit.schedule;

import java.util.EventListener;

/**
 * 任务执行监听
 */
public interface ScheduleTaskListener extends EventListener {
	/**
	 * 任务开始时的监听
	 */
	void execueStart(ScheduleTask task);
	
	/**
	 * 任务执行成功的监听
	 */
	void executeSuccess(ScheduleTask task);
	
	/**
	 * 任务执行失败的监听
	 */
	void executeError(ScheduleTask task, Throwable t);
}
