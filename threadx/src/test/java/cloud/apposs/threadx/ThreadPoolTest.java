package cloud.apposs.threadx;

import static org.junit.Assert.assertEquals;

import cloud.apposs.threadx.sample.ProcessableSample;
import cloud.apposs.threadx.sample.ProcessorSample;
import cloud.apposs.threadx.sample.RunnableSample;
import cloud.apposs.threadx.sample.ThreadServiceListenerSample;
import org.junit.Test;

import cloud.apposs.threadx.ThreadService.Task;

public class ThreadPoolTest {
	public static void main(String[] args) {
		testProcessorTaskShutdownNow();
	}
	
	public static void testProcessorTask() {
		ThreadService pool = new ThreadPool();
		for (int i = 0; i < 20; i++) {
			pool.execute(new ProcessorSample());
		}
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testProcessableTask() {
		long start = System.currentTimeMillis();
		ThreadService pool = new ThreadPool();
		for (int i = 0; i < 20; i++) {
			Future<String> future = pool.submit(new ProcessableSample());
			future.addListener(new FutureListener<Future<?>>() {
				@Override
				public void executeComplete(Future<?> future, Throwable cause) {
					System.out.println("Future Execute done:" + future.getNow());
				}
			});
		}
		pool.shutdown();
		System.out.println("主线程执行结束");
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	public static void testProcessorTaskShutdownNow() {
		ThreadService pool = new ThreadPool();
		for (int i = 0; i < 20; i++) {
			pool.execute(new ProcessorSample());
		}
		pool.shutdownNow();
		System.out.println("主线程执行结束");
	}
	
	public static void testExecuteTask() {
		ThreadService pool = new ThreadPool();
		for (int i = 0; i < 2; i++) {
			pool.execute(new RunnableSample());
		}
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testGetIdleTaskToAddProcessor() {
		ThreadService pool = new ThreadPool();
		Task task = pool.getIdleTask();
		task.addProcessor(new ProcessorSample());
	}
	
	public static void testGetIdleTaskToAddExecutor() {
		ThreadService pool = new ThreadPool();
		Task task = pool.getIdleTask();
		task.addExecutor(new RunnableSample());
	}
	
	public static void testGetIdleTaskWithWaitTime() {
		ThreadService pool = new ThreadPool(1, 1);
		Task task = pool.getIdleTask();
		task.addProcessor(new ProcessorSample());
		Task task2 = pool.getIdleTask(6000);
		System.out.println("the task is : " + task2);
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolShutdown() {
		ThreadService pool = new ThreadPool();
		Task task = pool.getIdleTask();
		task.addProcessor(new ProcessorSample());
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolShutdownAndGetAgain() {
		ThreadService pool = new ThreadPool();
		Task task = pool.getIdleTask();
		task.addProcessor(new ProcessorSample());
		pool.shutdown();
		Task task2 = pool.getIdleTask();
		task2.addProcessor(new ProcessorSample());
		System.out.println("主线程执行结束");
	}
	
	@Test
	public void testGetIdleTaskBlock() {
		ThreadService pool = new ThreadPool(1, 2);
		assertEquals(false, pool.isExhausted());
		Task task = pool.getIdleTask();
		assertEquals(false, pool.isExhausted());
		task.addProcessor(new ProcessorSample());
		Task task2 = pool.getIdleTask();
		task2.addProcessor(new ProcessorSample());
		assertEquals(true, pool.isExhausted());
		Task task3 = pool.getIdleTask();
		task3.addProcessor(new ProcessorSample());
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	@Test
	public void testThreadPoolStatics() {
		ThreadService pool = new ThreadPool();
		for (int i = 0; i < 4; i++) {
			Task task = pool.getIdleTask();
			task.addProcessor(new ProcessorSample());
		}
		assertEquals(4, pool.getNumBusy());
		pool.shutdown();
		try {
			pool.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(0, pool.getNumBusy());
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolAwaitTermination() {
		ThreadService pool = new ThreadPool();
		Task task = pool.getIdleTask();
		task.addProcessor(new ProcessorSample());
		pool.shutdown();
		try {
			while (!pool.awaitTermination(1000)) {
				System.out.println("任务未完成");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("主线程执行结束");
	}
	
	public static void getThreadPoolTasks() {
		ThreadPool pool = new ThreadPool();
		for (Task task : pool.getTasks()) {
			System.out.println(task);
		}
	}
	
	public static void printThreadPoolInfo() {
		ThreadPool pool = new ThreadPool();
		System.out.println(pool);
	}
	
	public static void testThreadPoolFactory() {
		ThreadService pool = ThreadPoolFactory.createSingleThreadPool();
		for (int i = 0; i < 2; i++) {
			pool.execute(new ProcessorSample());
		}
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolCorePoolSize() {
		ThreadService pool = new ThreadPool(1, 10);
		System.out.println(pool.getNumIdle());
		for (int i = 0; i < 11; i++) {
			pool.execute(new ProcessorSample());
		}
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(pool.getNumIdle());
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolAdjust() {
		ThreadPool pool = new ThreadPool(1, 10);
		for (int i = 0; i < 11; i++) {
			pool.execute(new ProcessorSample());
		}
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		pool.adjust();
		System.out.println(pool.getNumIdle());
		pool.shutdown();
		System.out.println("主线程执行结束");
	}
	
	public static void testThreadPoolListener() {
		ThreadService pool = ThreadPoolFactory.createCachedThreadPool();
		pool.addListener(new ThreadServiceListenerSample());
		for (int i = 0; i < 11; i++) {
			pool.execute(new ProcessorSample());
		}
		pool.shutdown();
	}
}
