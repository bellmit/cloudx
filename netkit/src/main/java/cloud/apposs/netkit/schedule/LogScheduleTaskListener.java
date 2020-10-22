package cloud.apposs.netkit.schedule;

import cloud.apposs.logger.Logger;

public class LogScheduleTaskListener implements ScheduleTaskListener {
	@Override
	public void execueStart(ScheduleTask task) {
		Logger.info("task execute start;name=%s;times=%d;flow=%d;",
				task.getName(), task.getTimes(), task.getFlow());		
	}

	@Override
	public void executeError(ScheduleTask task, Throwable t) {
		Logger.error(t, "task execute error;name=%s;times=%d;flow=%d;", 
				task.getName(), task.getTimes(), task.getFlow());
	}

	@Override
	public void executeSuccess(ScheduleTask task) {
		Logger.info("task execute ok;name=%s;times=%d;flow=%d;", 
				task.getName(), task.getTimes(), task.getFlow());
	}
}
