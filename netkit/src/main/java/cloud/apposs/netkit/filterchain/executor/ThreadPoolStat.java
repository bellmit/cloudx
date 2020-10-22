package cloud.apposs.netkit.filterchain.executor;

import java.util.List;

import cloud.apposs.netkit.filterchain.executor.ThreadPool.Worker;

/**
 * 线程池状态检查服务，定时检查线程
 * 1、线程存活状态
 * 2、线程执行是否超时
 * 3、线程是否繁忙(即线程数是否已满)
 */
public final class ThreadPoolStat extends Thread {
	private final ThreadPool pool;
	
	private final int interval;
	
	private final int timeout;
	
	private volatile boolean running = false;
	
	private final ThreadPoolListenerSupport listener;
	
	public ThreadPoolStat(ThreadPool pool, int interval, int timeout, ThreadPoolListenerSupport listener) {
		this.setDaemon(true);
		this.pool = pool;
		this.interval = interval;
		this.timeout = timeout;
		this.listener = listener;
	}
	
	public synchronized void shutdown() {
		running = false;
		interrupt();
	}

	@Override
	public void run() {
		running = true;
		while(running) {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				if (!running) break;
				continue;
			}
			
			// 检查线程是否存活
			List<Worker> workerList = pool.getWorkerList();
			int workerCount = workerList.size();
			for (int i = workerList.size() - 1; i >= 0; --i) {
				Worker worker = workerList.get(i);
				if (worker.isAlive()) {
					continue;
				}
				
				workerList.remove(i);
			}
			int createCount = workerCount - workerList.size();
			// 重建线程
			for (int i = 0; i < createCount; i++) {
				Worker worker = pool.doCreateThread();
				listener.fireWorkerDead(worker);
			}
			
			// 检查线路是否繁忙
			int workingCount = pool.getWorkingCount();
			if (workingCount > (workerCount / 4) * 3) {
				listener.fireWorkerBusy();
			}
			
			// 检查线程是否执行超时
			workerList = pool.getWorkerList();
			for (int i = 0; i < workerList.size(); i++) {
				Worker worker = workerList.get(i);
				int exetime = worker.getExeTime();
				if (exetime > 0 && timeout > 0 && exetime > timeout) {
					listener.fireWorkerTimeout(worker);
				}
			}
		}
	}
}
