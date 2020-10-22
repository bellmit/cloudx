package cloud.apposs.threadx.sample;

import cloud.apposs.threadx.Processor;
import cloud.apposs.threadx.ThreadService;

public class ProcessorSample implements Processor {
	@Override
	public void process(ThreadService.ThreadContext context) {
		try {
			System.out.println("Processor任务[" + Thread.currentThread().getName() + "]执行开始");
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Processor任务[" + Thread.currentThread().getName() + "]执行结束");
	}
}
