package cloud.apposs.threadx.sample;

import cloud.apposs.threadx.Processable;
import cloud.apposs.threadx.ThreadService;

public class ProcessableSample implements Processable<String> {
	@Override
	public String process(ThreadService.ThreadContext context) throws Exception {
		try {
			System.out.println("Processable任务[" + Thread.currentThread().getName() + "]执行开始");
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Processable任务[" + Thread.currentThread().getName() + "]执行结束");
		return "OK";
	}
}
