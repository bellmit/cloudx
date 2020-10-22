package cloud.apposs.threadx.sample;

import cloud.apposs.threadx.Processor;
import cloud.apposs.threadx.ThreadService;
import cloud.apposs.threadx.ThreadServiceListener;

public class ThreadServiceListenerSample implements ThreadServiceListener {
	@Override
	public void serviceExhausted(ThreadService service) {
		System.out.println("thread pool:" + service + " exhausted");
	}

	@Override
	public void serviceShutdown(ThreadService service) {
		System.out.println("thread pool:" + service + " shutdown");
	}

	@Override
	public void serviceTerminated(ThreadService service) {
		System.out.println("thread pool:" + service + " terminated");
	}
	
	@Override
	public void beforeThreadProcess(Thread worker, Processor task) {
		System.out.println("task:" + task + " is going to process");
	}

	@Override
	public void afterThreadProcess(Thread worker, Processor task, Throwable cause) {
		System.out.println("task:" + task + " process finished");
	}

	@Override
	public void afterThreadExecute(Thread worker, Runnable task, Throwable cause) {
		System.out.println("task:" + task + " is going to execute");
	}

	@Override
	public void beforeThreadExecute(Thread worker, Runnable task) {
		System.out.println("task:" + task + " execute finished");
	}
}
