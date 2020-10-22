package cloud.apposs.threadx.sample;

public class RunnableSample implements Runnable {
	@Override
	public void run() {
		try {
			System.out.println("Runnable任务[" + Thread.currentThread().getName() + "]执行开始");
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Runnable任务[" + Thread.currentThread().getName() + "]执行结束");
	}
}
