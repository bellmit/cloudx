package cloud.apposs.threadx;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认线程生成器，业务自定义或者基于此类直接创建
 */
public class DefaultThreadFactory implements ThreadFactory {
    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    public DefaultThreadFactory() {
        this(null);
    }

    public DefaultThreadFactory(String namePrefix) {
        SecurityManager s = System.getSecurityManager();
        this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        if (namePrefix != null && !namePrefix.trim().isEmpty()) {
            this.namePrefix = namePrefix;
        } else {
            this.namePrefix = "ThreadPool-" + poolNumber.getAndIncrement() + "-";
        }
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
