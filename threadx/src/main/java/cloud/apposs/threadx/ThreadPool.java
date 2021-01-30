package cloud.apposs.threadx;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * 线程池，用于管理所有线程，
 * 与Concurrent不同的是该线程是通过{@link #getIdleTask()}的方式获取空闲线程再执行任务
 *
 * @author wayken.hong@gmail.com
 * @date 2018.09.23
 */
public class ThreadPool implements ThreadService, ThreadPoolMBean {
    private final ThreadContext context = new ThreadContext(this);

    /**
     * 空闲线程队列
     */
    private final Queue<Task> tasks = new ConcurrentLinkedQueue<Task>();
    /**
     * 正在执行任务的线程队列
     */
    private final Queue<Task> workers = new ConcurrentLinkedQueue<Task>();

    /**
     * 所有线程池监听器
     */
    private final ThreadServiceListenerSupport listeners = new ThreadServiceListenerSupport(this);
    ;

    /**
     * 线程池同步锁
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * 线程终结的条件锁
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * 取出空闲线程时的阻塞锁
     */
    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();

    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    /**
     * 核心线程数，线程池中运行的线程不得少于corePoolSize
     */
    private volatile int corePoolSize = DEFAULT_CORE_POOL_SIZE;
    public static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程（空闲线程数和忙碌线程数的总和），超过最大线程数则无法再添加线程到线程池中
     */
    private volatile int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    public static final int DEFAULT_MAX_POOL_SIZE = Integer.MAX_VALUE;

    private ThreadFactory threadFactory = DEFAULT_THREAD_FACTORY;
    public static final ThreadFactory DEFAULT_THREAD_FACTORY = new DefaultThreadFactory();

    private RejectedExecutionHandler handler = DEFAULT_REJECTED_EXECUTION_HANDLER;
    public static final RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = new AbortPolicy();

    /**
     * 忙碌线程数
     */
    private volatile int numBusy = 0;

    /**
     * 线程池状态
     * RUNNING:  	正在运行
     * SHUTDOWN:	正在关闭，但未执行完成的任务还在执行
     * STOP:		完全关闭，即使有未执行的任务也直接退出
     * TERMINATED:线程池完全关闭，所有任务已执行完成
     */
    private volatile int runState = RUNNING;
    public static final int RUNNING = 0;
    public static final int SHUTDOWN = 1;
    public static final int TERMINATED = 2;

    /**
     * 是否开启JMX监控
     */
    private boolean enableJMX = DEFAULT_ENABLE_JMX;
    public static final boolean DEFAULT_ENABLE_JMX = false;

    /**
     * JMX常量
     */
    public static final String MBEAN_THREAD_POOL =
            ThreadPool.class.getPackage().getName() + ":type=ThreadPoolMBean";

    public ThreadPool() {
        this(DEFAULT_CORE_POOL_SIZE);
    }

    public ThreadPool(int corePoolSize) {
        this(corePoolSize, DEFAULT_MAX_POOL_SIZE, DEFAULT_ENABLE_JMX,
                DEFAULT_THREAD_FACTORY, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public ThreadPool(int corePoolSize, int maxPoolSize) {
        this(corePoolSize, maxPoolSize, DEFAULT_ENABLE_JMX,
                DEFAULT_THREAD_FACTORY, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public ThreadPool(int corePoolSize, int maxPoolSize, ThreadFactory threadFactory) {
        this(corePoolSize, maxPoolSize, DEFAULT_ENABLE_JMX,
                threadFactory, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public ThreadPool(int corePoolSize, int maxPoolSize, boolean enableJMX) {
        this(corePoolSize, maxPoolSize, enableJMX,
                DEFAULT_THREAD_FACTORY, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public ThreadPool(int corePoolSize, int maxPoolSize, boolean enableJMX,
                      ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize <= 0 || corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException();
        }
        if (threadFactory == null || handler == null) {
            throw new NullPointerException();
        }
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
    	this.threadFactory = threadFactory;
        this.enableJMX = enableJMX;
        this.handler = handler;
        // 添加任务
        for (int i = 0; i < corePoolSize; i++) {
            doAddTask();
        }
        // 初始化JMX
        if (enableJMX) {
            doInitJMX();
        }
    }

    @Override
    public Task getIdleTaskNow() {
        return tasks.poll();
    }

    @Override
    public Task getIdleTask() {
        return getIdleTask(-1);
    }

    @Override
    public Task getIdleTask(long waitTime) {
        Task task = doGetTask(waitTime);

        if (runState != RUNNING) {
            handler.rejectedExecution(task, this);
        }

        boolean success = true;
        if (task != null) {
            task.active();
            if (!task.validate()) {
                task.destroy(false);
                success = false;
            }
        } else {
            success = false;
        }

        if (success) {
            return task;
        }
        return null;
    }

    @Override
    public void execute(Processor processor) {
        Task task = getIdleTask();
        if (task == null) {
            handler.rejectedExecution(task, this);
        }
        task.addProcessor(processor);
    }

    @Override
    public void execute(Runnable runnable) {
        Task task = getIdleTask();
        if (task == null) {
            handler.rejectedExecution(task, this);
        }
        task.addExecutor(runnable);
    }

    @Override
    public <V> FutureTask<V> submit(Callable<V> callable) {
        Task task = getIdleTask();
        if (task == null) {
            handler.rejectedExecution(task, this);
        }
        FutureTask<V> fuctureTask = new FutureTask<V>(callable);
        task.addExecutor(fuctureTask);
        return fuctureTask;
    }

    @Override
    public <V> FutureTask<V> submit(Processable<V> processable) {
        Task task = getIdleTask();
        if (task == null) {
            handler.rejectedExecution(task, this);
        }
        FutureTask<V> fuctureTask = new FutureTask<V>(processable);
        task.addProcessor(fuctureTask);
        return fuctureTask;
    }

    @Override
    public void addListener(ThreadServiceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ThreadServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * 返回核心线程数
     *
     * @return 核心线程数
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 返回最大线程数
     *
     * @return 核心线程数
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public Collection<Task> getTasks() {
        return Collections.unmodifiableCollection(tasks);
    }

    @Override
    public int getNumIdle() {
        return tasks.size();
    }

    @Override
    public int getNumBusy() {
        return numBusy;
    }

    @Override
    public boolean isExhausted() {
        // 只有当空闲线程为0并且忙碌线程数大于最大线程数时判断为资源耗竭
        return tasks.isEmpty() && (numBusy >= maxPoolSize);
    }

    @Override
    public final boolean isShutdown() {
        return runState != RUNNING;
    }

    @Override
    public final boolean isTerminated() {
        return runState == TERMINATED;
    }

    @Override
    public boolean awaitTermination(long timeout) throws InterruptedException {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (runState == TERMINATED)
                    return true;
                if (timeout > 0) {
                    // 阻塞等待，直到时间超时或者全部任务执行完成
                    return termination.awaitNanos(TimeUnit.MILLISECONDS.toNanos(timeout)) > 0 ? true : false;
                } else {
                    termination.await();
                    return true;
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        awaitTermination(-1);
    }

    /**
     * 调整线程池
     * 如果线程池中空闲线程数大于核心线程数则执行多余线程销毁
     * 只保证空闲线数稳定在核心线程数且不超过最大线程数
     * 注意！调用此方法时最好保证大部分线程为空闲状态以便于回收更多空闲线程
     */
    public void adjust() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            while (tasks.size() > corePoolSize) {
                tasks.poll().destroy(false);
            }
        } finally {
            mainLock.unlock();
        }
    }

    public boolean isEnableJMX() {
        return enableJMX;
    }

    @Override
    public void shutdown() {
        shutdown(false);
    }

    @Override
    public void shutdownNow() {
        shutdown(true);
    }

    @Override
    public void shutdown(boolean interrupt) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
        }
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (security != null) {
                for (Task task : tasks) {
                    security.checkAccess(task.thread);
                }
                for (Task task : workers) {
                    security.checkAccess(task.thread);
                }
            }

            int state = runState;
            if (state < SHUTDOWN) {
                runState = SHUTDOWN;
            }

            listeners.fireServiceShutdown();

            Iterator<Task> taskIt = tasks.iterator();
            while (taskIt.hasNext()) {
                taskIt.next().destroy(interrupt);
                taskIt.remove();
            }
            if (interrupt) {
                // 中断目前正在执行的任务
                Iterator<Task> workerIt = workers.iterator();
                while (workerIt.hasNext()) {
                    workerIt.next().destroy(interrupt);
                    workerIt.remove();
                }
            }

            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 添加任务，提交一个任务用于执行，为系统内部调用
     */
    private void doAddTask() {
        // 新建任务并启动线程
        Task task = new Task();
        Thread thread = threadFactory.createThread(task);
        task.thread = thread;
        tasks.offer(task);
        thread.start();
        // 通知其他有可能锁定的线程此时线程池非空
        signalNotEmpty();
    }

    /**
     * 初始化JMX监控
     */
    private void doInitJMX() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName threadPoolname = new ObjectName(MBEAN_THREAD_POOL);
            if (!mbs.isRegistered(threadPoolname)) {
                mbs.registerMBean(this, threadPoolname);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 任务执行完成时触发的回调
     *
     * @param task 完成的任务
     */
    private void fireTaskDone(Task task) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            retrieveTask(task);
            if (runState == SHUTDOWN) {
                tryTerminate();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 线程回收，即任务执行完成时回收到线程池中，为系统内部调用
     *
     * @param task 要回收的任务
     */
    private void retrieveTask(Task task) {
        if (task == null) {
            throw new NullPointerException();
        }

        boolean success = true;
        task.passive();
        if (!task.validate()) {
            task.destroy(false);
            success = false;
        }

        // 任务验证通过，回收到线程池中
        if (success) {
            workers.remove(task);
            tasks.add(task);
            signalNotEmpty();
        }
    }

    /**
     * 获取线程池中空闲线程，如果空闲线程为空且线程池资源未耗竭则自动创建并返回
     * 否则阻塞等待直到有任务执行完成回收到线程池中
     *
     * @param waitTime 等待超时时间
     * @return 线程池中空闲线程
     */
    private Task doGetTask(long waitTime) {
        checkTasks();
        Task task = null;
        final ReentrantLock takeLock = this.takeLock;
        try {
            takeLock.lockInterruptibly();
            while (tasks.size() == 0) {
                if (waitTime > 0) {
                    notEmpty.await(waitTime, TimeUnit.MILLISECONDS);
                    break;// 到达超时时间，无论空闲队列有没有空闲线程都退出
                } else {
                    notEmpty.await();
                }
            }
            task = tasks.size() > 0 ? tasks.poll() : null;
        } catch (InterruptedException e) {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        return task;
    }

    /**
     * 检查线程池，当线程池资源未耗竭并且空闲线程时小于核心线程数时，系统自动创建并添加空闲线程到线程池中
     */
    private void checkTasks() {
        if (!isExhausted() && tasks.size() < corePoolSize) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (tasks.isEmpty() || tasks.size() < corePoolSize) {
                    doAddTask();
                }
            } finally {
                mainLock.unlock();
            }
        } else if (isExhausted()) {
            // 线程池资源已经耗竭
            listeners.fireServiceExhausted();
        }
    }

    /**
     * 告知线程池此时线程池中空闲线程不为空啦
     */
    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * 在线程池已关闭且所有任务都执行完成的情况下将线程池状态改为TERMINATED，彻底终结线程池
     */
    private void tryTerminate() {
        if (runState == SHUTDOWN && numBusy == 0) {
            runState = TERMINATED;
            termination.signalAll();
            listeners.fireServiceTerminated();
        }
    }

    @Override
    public String toString() {
        StringBuffer info = new StringBuffer();
        info.append("{");
        info.append("taskIdle:");
        StringBuffer taskInfo = new StringBuffer();
        for (Task task : tasks) {
            taskInfo.append(task).append(", ");
        }
        if (taskInfo.length() > 0) {
            info.append(taskInfo.substring(0, taskInfo.lastIndexOf(","))).append(", ");
        }
        info.append("numBusy:").append(numBusy).append(", ");
        info.append("shutdown:").append(runState == SHUTDOWN ? true : false).append(", ");
        info.append("terminated:").append(runState == TERMINATED ? true : false);
        info.append("}");
        return info.toString();
    }

    public final class Task implements ThreadService.Task {
        private Processor processor;

        private Runnable runnable;

        private volatile boolean running = true;

        private Thread thread;

        @Override
        public void run() {
            while (running) {
                if (processor == null && runnable == null) {
                    // 没任务执行时是任务线程阻塞等待状态
                    try {
                        synchronized (this) {
                            // 注意！ isShutdown判断很重要，如果不加判断，线程池关闭时此方法将有可能死锁
                            // 具体原因为当线程池关闭并notify其他线程时，此时线程有可能仍未wait阻塞
                            // 等进入wait方法时因为线程池已关闭不会再notify此线程，所以会一直阻塞等待
                            if (isShutdown()) {
                                break;
                            }
                            // 阻塞等有任务触发
                            wait();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }

                // 空闲线程被唤醒，开始执行任务
                if (processor != null) {
                    doProcess();
                } else if (runnable != null) {
                    doExecute();
                }
            }
        }

        private void doProcess() {
            // 已经被取出并开始执行任务
            try {
                listeners.fireBeforeThreadProcess(thread, processor);
                // 开始执行任务
                processor.process(context);
                listeners.fireAfterThreadProcess(thread, processor, null);
            } catch (Throwable t) {
                listeners.fireAfterThreadExecute(thread, runnable, t);
            } finally {
                // 任务执行结束
                fireTaskDone(this);
            }
        }

        private void doExecute() {
            // 已经被取出并开始执行任务
            try {
                // 开始执行任务
                listeners.fireBeforeThreadExecute(thread, runnable);
                runnable.run();
                listeners.fireAfterThreadExecute(thread, runnable, null);
            } catch (Throwable t) {
                listeners.fireAfterThreadExecute(thread, runnable, t);
            } finally {
                // 任务执行结束
                fireTaskDone(this);
            }
        }

        @Override
        public boolean validate() {
            return !isShutdown();
        }

        @Override
        public void active() {
            // 空闲线程被取出，忙碌线程数递增
            numBusy++;
        }

        @Override
        public void passive() {
            processor = null;
            runnable = null;
            // 空闲线程回收到线程池，忙碌线程数递减
            ThreadPool.this.numBusy--;
        }

        @Override
        public void destroy(boolean interrupt) {
            if (running) {
                running = false;
                // 唤醒线程，结束轮询
                synchronized (this) {
                    notify();
                }
                if (interrupt) {
                    thread.interrupt();
                }
            }
        }

        @Override
        public void addProcessor(Processor processor) {
            if (processor == null) {
                throw new NullPointerException();
            }
            workers.add(this);
            this.processor = processor;
            // 唤醒线程，开始执行任务
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void addExecutor(Runnable runnable) {
            if (runnable == null) {
                throw new NullPointerException();
            }
            workers.add(this);
            this.runnable = runnable;
            // 唤醒线程，开始执行任务
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append("{");
            str.append("name:").append(thread.getName()).append(", ");
            str.append("running:").append(running);
            str.append("}");
            return str.toString();
        }
    }

    public static class AbortPolicy implements RejectedExecutionHandler {
        public AbortPolicy() {
        }

        @Override
        public void rejectedExecution(ThreadService.Task task, ThreadService pool) {
            throw new RejectedExecutionException();
        }
    }
}
