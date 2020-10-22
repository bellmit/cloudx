package cloud.apposs.netkit.server.fai;

import cloud.apposs.netkit.schedule.LogScheduleTaskListener;
import cloud.apposs.netkit.schedule.ScheduleTask;

public abstract class FaiScheduleTask extends ScheduleTask {
	public FaiScheduleTask(String name) {
		super(name);
		listeners.add(new LogScheduleTaskListener());
	}
}
