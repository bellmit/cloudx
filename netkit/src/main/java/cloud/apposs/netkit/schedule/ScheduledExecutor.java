package cloud.apposs.netkit.schedule;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ScheduledExecutor {
	private final ScheduledThreadPoolExecutor executor;
	
	/** 
	 * 周期性执行定时任务表
	 */
	private final Map<String, ScheduledFuture<?>> scheduleTasks = new ConcurrentHashMap<String, ScheduledFuture<?>>();
	
	public ScheduledExecutor(int corePoolSize) {
		this.executor = new ScheduledThreadPoolExecutor(corePoolSize);
	}
	
	/**
	 * 按指定频率周期执行任务，
	 * 初始化延迟delay开始执行，每隔period重新执行一次任务
	 */
	public ScheduledFuture<?> scheduleRatePeriod(ScheduleTask task, long delay, long period) {
		ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
		scheduleTasks.put(task.getName(), future);
		return future;
	}
	
	/**
	 * 按指定频率周期执行任务，
	 * 初始化时延时delay开始执行，本次执行结束后延迟period开始下次执行
	 */
	public ScheduledFuture<?> scheduleDelayPeriod(ScheduleTask task, long delay, long period) {
		ScheduledFuture<?> future = executor.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
		scheduleTasks.put(task.getName(), future);
		return future;
	}
	
	public ScheduledFuture<?> scheduleDayPeriod(ScheduleTask task, int hour) {
		final int dayPeriod = 24 * 60 * 60 * 1000;
		return schedulePeriod(task, hour, 0, 0, dayPeriod, false);
	}
	
	public ScheduledFuture<?> scheduleDayPeriod(ScheduleTask task, int hour, boolean immediately) {
		final int dayPeriod = 24 * 60 * 60 * 1000;
		return schedulePeriod(task, hour, 0, 0, dayPeriod, immediately);
	}
	
	public ScheduledFuture<?> scheduleDayPeriod(ScheduleTask task, int hour, int minute, int second) {
		final int dayPeriod = 24 * 60 * 60 * 1000;
		return schedulePeriod(task, hour, minute, second, dayPeriod, false);
	}
	
	/**
	 * 设置每天多少点执行定时任务
	 * 
	 * @param task 执行任务 
	 * @param hour 执行的小时数
	 * @param minute 执行的分钟数
	 * @param second 执行的秒数
	 * @param immediately true为立即执行并在第二天指定时间再执行，false则等待到指定时间再执行
	 */
	public ScheduledFuture<?> scheduleDayPeriod(ScheduleTask task, int hour, int minute, int second, boolean immediately) {
		final int period = 24 * 60 * 60 * 1000;
		return schedulePeriod(task, hour, minute, second, period, immediately);
	}
	
	public ScheduledFuture<?> scheduleMinutePeriod(ScheduleTask task, int hour, int minute, int second, boolean immediately) {
		final int period = 60 * 1000;
		return schedulePeriod(task, hour, minute, second, period, immediately);
	}
	
	public ScheduledFuture<?> scheduleHourPeriod(ScheduleTask task, int hour, int minute, int second, boolean immediately) {
		final int period = 60 * 60 * 1000;
		return schedulePeriod(task, hour, minute, second, period, immediately);
	}

	public ScheduledFuture<?> schedulePeriod(ScheduleTask task, 
			int hour, int minute, int second, long period, boolean immediately) {
		if (immediately) {
			final ScheduledFuture<?> future = executor.scheduleWithFixedDelay(task, 0, period, TimeUnit.MILLISECONDS);
			task.addListener(new ScheduleTaskListener() {
				@Override
				public void executeSuccess(ScheduleTask task) {
					future.cancel(true);
				}
				
				@Override
				public void executeError(ScheduleTask task, Throwable t) {
					future.cancel(true);
				}
				
				@Override
				public void execueStart(ScheduleTask task) {
					if (task.getTimes() > 0) {
						future.cancel(true);
					}
				}
			});
		}
		
		Date executeDate = getDateAt(new Date(), hour, minute, second);
		long delay  = executeDate.getTime() - System.currentTimeMillis();
		delay = delay > 0 ? delay : period + delay; 
		ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);
		scheduleTasks.put(task.getName(), future);
		return future;
	}
	
	public void shutdown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}
	
	public void cancleTask(String taskName) {
		ScheduledFuture<?> taskFuture = scheduleTasks.get(taskName);
		if (taskFuture != null) {
			if (!taskFuture.isCancelled()) {
				taskFuture.cancel(true);
			}
			scheduleTasks.remove(taskName);
		}
	}
	
	public static Date getDateAt(Date date, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
	}
}
