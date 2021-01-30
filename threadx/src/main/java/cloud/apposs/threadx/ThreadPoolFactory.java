package cloud.apposs.threadx;

/**
 * 线程池工厂，负责创建不同功能的线程池
 */
public class ThreadPoolFactory {
	private static final int MAX_THREAD_NUM = Byte.MAX_VALUE;

    private ThreadPoolFactory() {
    }

    /**
     * 创建一个可根据需要对线程自动控制的线程池
     *
     * @return 新创建的线程池
     */
    public static ThreadPool createCachedThreadPool() {
        return new ThreadPool(Runtime.getRuntime().availableProcessors(), MAX_THREAD_NUM);
    }

    /**
     * 创建线程池
     *
     * @param threadFactory 自定义线程创建工厂，方便通过jstack查看线程信息
     * @return
     */
    public static ThreadPool createCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPool(Runtime.getRuntime().availableProcessors(), MAX_THREAD_NUM, threadFactory);
    }

    /**
     * 创建线程池
     *
     * @param threadNamePrefix 线程名前缀，方便通过jstack查看线程信息
     */
    public static ThreadPool createCachedThreadPool(String threadNamePrefix) {
        return new ThreadPool(Runtime.getRuntime().availableProcessors(),
                MAX_THREAD_NUM, new DefaultThreadFactory(threadNamePrefix));
    }

    /**
     * 创建一个固定数量线程集合的线程池
     * 以共享的无界队列方式来运行这些线程
     *
     * @param threadNum 池中的线程数
     * @return 新创建的线程池
     */
    public static ThreadPool createFixedThreadPool(int threadNum) {
        return new ThreadPool(threadNum, threadNum);
    }

    public static ThreadPool createFixedThreadPool(int threadNum, ThreadFactory threadFactory) {
        return new ThreadPool(threadNum, threadNum, threadFactory);
    }

    /**
     * 创建一个使用单个线程的线程池
     * 单工作线程最大的特点是可保证顺序地执行各个任务，并且在任意给定的时间不会有多个线程是活动的
     *
     * @return 新创建的线程池
     */
    public static ThreadPool createSingleThreadPool() {
        return new ThreadPool(1, 1);
    }

    public static ThreadPool createSingleThreadPool(ThreadFactory threadFactory) {
        return new ThreadPool(1, 1, threadFactory);
    }
}
