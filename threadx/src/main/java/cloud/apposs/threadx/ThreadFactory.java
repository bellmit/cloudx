package cloud.apposs.threadx;

/**
 * 线程池工作线创建工厂
 */
public interface ThreadFactory {
	Thread createThread(Runnable r);
}
