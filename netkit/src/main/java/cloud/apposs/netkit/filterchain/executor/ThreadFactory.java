package cloud.apposs.netkit.filterchain.executor;

/**
 * 线程池工作线创建工厂
 */
public interface ThreadFactory {
	Thread createThread(Runnable r);
}
