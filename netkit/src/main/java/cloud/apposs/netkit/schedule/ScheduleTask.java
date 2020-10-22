package cloud.apposs.netkit.schedule;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ScheduleTask implements Runnable {
	protected final List<ScheduleTaskListener> listeners = new CopyOnWriteArrayList<ScheduleTaskListener>();
	
	protected final String name;
	
	/**
	 * 执行流水号
	 */
	protected long flow;
	
	/**
	 * 执行是否成功
	 */
	protected boolean success = true;
	
	/**
	 * 执行总次数
	 */
	protected long times = 0;

	public ScheduleTask(String name) {
		this(name, 10000L);
	}
	
	public ScheduleTask(String name, long flow) {
		this.name = name;
		this.flow = flow;
	}

	public String getName() {
		return name;
	}
	
	public long getFlow() {
		return flow;
	}
	
	public long getTimes() {
		return times;
	}

	public void addListener(ScheduleTaskListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(ScheduleTaskListener listener) {
		listeners.remove(listener);
	}

	public boolean isSuccess() {
		return success;
	}

	@Override
	public void run() {
		try {
			for (ScheduleTaskListener listener : listeners) {
	        	listener.execueStart(this);
	        }
			
			doExecute();
			success = true;
			
			for (ScheduleTaskListener listener : listeners) {
	        	listener.executeSuccess(this);
	        }
		} catch(Throwable t) {
			success = false;
			for (ScheduleTaskListener listener : listeners) {
	        	listener.executeError(this, t);
	        }
		} finally {
			flow++;
			times++;
		}
	}
	
	public abstract void doExecute();
}
