package cloud.apposs.netkit.filterchain.executor;

import cloud.apposs.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPool {
	private final String name;
	
	/** 线程数 */
	public static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private volatile int poolSize = DEFAULT_POOL_SIZE;

    private final ThreadFactory threadFactory;
    
    private final Map<String, TaskStat> taskMap;
    private final BlockingQueue<Task> taskQueue;
    
    private final List<Worker> workerList;
    
    private volatile boolean shutdown;
    private static final Task EXIT_SIGNAL = new ExitTask();
	
    public ThreadPool(String name) {
    	this(name, DEFAULT_POOL_SIZE,  new DefaultThreadFactory(name));
    }
    
    public ThreadPool(String name, int poolSize) {
    	this(name, poolSize,  new DefaultThreadFactory(name));
    }
    
	public ThreadPool(String name, int poolSize, ThreadFactory threadFactory) {
		if (poolSize <= 0) {
			throw new IllegalArgumentException("poolSize");
		}
		if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
		
		this.name = name;
		this.poolSize = poolSize;
		this.threadFactory = threadFactory;
		this.taskMap = new ConcurrentHashMap<String, TaskStat>();
		this.taskQueue = new LinkedBlockingQueue<Task>();
		this.workerList = new ArrayList<Worker>(poolSize);
		doInit();
	}
	
	public String getName() {
		return name;
	}

	public void execute(Task task) {
		if (task == null) {
            throw new NullPointerException("task");
        }
		
		if (!StrUtil.isEmpty(task.getGroup())) {
			TaskStat taskStat = taskMap.get(task.getGroup());
			if (taskStat == null) {
				taskStat = new TaskStat(task.getGroup(), task.getLimit());
				taskMap.put(task.getGroup(), taskStat);
			} else {
				taskStat.total.incrementAndGet();
			}
		}
		taskQueue.offer(task);
	}
	
	/**
	 * 获取所有的工作线程
	 */
	public List<Worker> getWorkerList() {
		return workerList;
	}
	
	public synchronized void shutdown() {
		if (shutdown) {
            return;
        }
        shutdown = true;
		
		for (Worker worker : workerList) {
			worker.shutdown();
			taskQueue.offer(EXIT_SIGNAL);
		}
	}
	
	private void doInit() {
		for (int i = 0; i < poolSize; i++) {
			doCreateThread();
		}
	}
	
	protected Worker doCreateThread() {
		Worker worker = new Worker();
		Thread thread = threadFactory.createThread(worker);
		workerList.add(worker);
		thread.start();
		return worker;
	}
	
	public int getWorkingCount() {
		int count = 0;
		int size = workerList.size();
		for (int i = 0; i < size; ++i) {
			// 因为ServerStat中有可能会把已死的线程移除，所以这里要再检查
			if (i >= workerList.size()) {
				break;
			}
			Worker worker = workerList.get(i);
			if (worker.isWorking()) {
				++count;
			}
		}
		return count;
	}

	@Override
	public String toString() {
		return "ThreadPool[" + name + "]";
	}
	
	public static class DefaultThreadFactory implements ThreadFactory {
        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DefaultThreadFactory(String threadPoolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "Pool-" + threadPoolName + "-Task-";
        }

        public Thread createThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
	
	private static final class ExitTask extends AbstractTask {
		public ExitTask() {
			super(-1, null, -1);
		}

		@Override
		public void run() {
		}
	}
	
	final class Worker extends Thread {
		private volatile boolean running = false;
		
		private volatile boolean working = false;
		
		private volatile long exeStartTime;

		@Override
		public void run() {
			running = true;
			while(running) {
				try {
					Task task = taskQueue.take();
					if (task == null) {
						continue;
					}
					
					if (task == EXIT_SIGNAL) {
                        break;
                    }

					working = true;
					exeStartTime = System.currentTimeMillis();
					// 根据任务是否有分组来决定是不分组立即执行，还是分组限制执行
					try {
						if (StrUtil.isEmpty(task.getGroup())) {
							task.run();
						} else {
							TaskStat stat = taskMap.get(task.getGroup());
							boolean ok = stat.begin(task);
							try {
								if (ok) task.run();
							} finally {
								stat.finish(ok);
							}
						}
					} finally {
						working = false;
					}
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
		
		public boolean isWorking() {
			return working;
		}
		
		public int getExeTime() {
			if (!working) {
				return -1;
			}
			
			return (int) (System.currentTimeMillis() - exeStartTime);
		}
		
		public synchronized void shutdown() {
			running = false;
		}
	}
	
	private final class TaskStat {
		private final Queue<Task> waittingTasks = new ConcurrentLinkedQueue<Task>();
		
		private final String group;
		
		private final int limit;
		
		private AtomicInteger current = new AtomicInteger(0);
		
		private AtomicInteger total = new AtomicInteger(1);
		
		public TaskStat(String group, int limit) {
			this.group = group;
			this.limit = limit;
		}

		public synchronized boolean begin(Task task) {
			current.incrementAndGet();
			boolean available = current.get() <= limit;
			if (!available) {
				// 该任务组内要执行的任务数已到达上限，
				// 将未执行的任务添加到缓冲队列中，
				// 等待任务组内的任务执行完成之后才将未执行的任务重新添加到执行队列中
				waittingTasks.offer(task);
			}
			return available;
		}
		
		public synchronized void finish(boolean success) {
			current.decrementAndGet();
			if (success) {
				total.decrementAndGet();
				boolean complete = current.get() <= 0 && 
					total.get() <= 0 && waittingTasks.isEmpty();
				if (complete) {
					taskMap.remove(group);
				} else if (!waittingTasks.isEmpty()) {
					// 任务执行完毕，把缓冲队列中未执行的任务重新添加到执行队列中
					// 注意，队列是先进先出，所以未执行的任务可能要比其他任务要慢执行
					taskQueue.add(waittingTasks.poll());
				}
			}
		}
	}
}
