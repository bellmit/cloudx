package cloud.apposs.netkit.rxio.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 无锁化编程，将用到锁的地方均采用队列串行执行的方式来实际以实现无锁化编程，
 * 底层实现原理：
 * 1、根据lockKey区分不同的子业务锁
 * 2、不同的子业务锁扔到对应的队列中
 * 3、定时线程将子业务队列的请求一一取出并执行以实现子业务的串行化执行
 */
public final class Actor {
    public static final String DEFAULT_ACTOR_THREAD_PREFIX = "Actor-Worker-";
    public static final int DEFAULT_ACTOR_THREAD_NUM = Runtime.getRuntime().availableProcessors();

    private volatile boolean shutdown = false;

    private final BlockingQueue<ActorTask> taskPool = new LinkedBlockingQueue<ActorTask>();
    private final Map<ActorLock, TaskLock> locks = new ConcurrentHashMap<ActorLock, TaskLock>();

    private final List<Worker> workerList;

    private static final ActorTask EXIT_SIGNAL = new ExitTask();

    public Actor() {
        this(DEFAULT_ACTOR_THREAD_NUM, false, DEFAULT_ACTOR_THREAD_PREFIX);
    }

    public Actor(String threadNamePrefix) {
        this(DEFAULT_ACTOR_THREAD_NUM, false, threadNamePrefix);
    }

    public Actor(int poolSize) {
        this(poolSize, false, DEFAULT_ACTOR_THREAD_PREFIX);
    }

    public Actor(int poolSize, String threadNamePrefix) {
        this(poolSize, false, threadNamePrefix);
    }

    public Actor(int poolSize, boolean daemon) {
        this(poolSize, daemon, DEFAULT_ACTOR_THREAD_PREFIX);
    }

    /**
     * 创建异步锁框架
     * @param poolSize 执行异步任务的线程池，
     *                 主要利于在多个不同的{@link ActorLock}进行锁资源争抢时线程池可以同时执行不同锁的任务执行
     * @param daemon 是否为守护线程
     * @param threadNamePrefix 线程名称前缀，方便进行jstack调试
     */
    public Actor(int poolSize, boolean daemon, String threadNamePrefix) {
        workerList = new ArrayList<Worker>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            Worker worker = new Worker();
            Thread thread = new Thread(worker);
            thread.setDaemon(daemon);
            thread.setName(threadNamePrefix + i);
            workerList.add(worker);
            thread.start();
        }
    }

    public static ActorLock createLock(Object key) {
        return new ActorLock(key);
    }

    /**
     * 开始执行异步串行锁
     *
     * @param key 该业务的子业务锁，可以为AID或者AID+CMD等来保持同一子业务串行执行，
     *            注意该key如果是一个自定义对象，为了保定locks这个map能够找到类型相同的key，该key对象要实现hashCode和equals方法
     * @param task 异步锁拿到之后执行的回调方法
     */
    public void lock(ActorLock key, ActorTask task) {
        TaskLock lock = locks.get(key);
        if (lock == null) {
            lock = new TaskLock(key, this);
            locks.put(key, lock);
        }
        key.setLock(lock);
        taskPool.offer(task);
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        for (Worker worker : workerList) {
            worker.shutdown();
            taskPool.offer(EXIT_SIGNAL);
        }
    }

    void removeLock(ActorLock key) {
        locks.remove(key);
    }

    void addTask(ActorTask task) {
        taskPool.add(task);
    }

    private static final class ExitTask implements ActorTask {
        @Override
        public ActorLock getLockKey() {
            return null;
        }

        @Override
        public void run() {
        }
    }

    final class Worker extends Thread {
        private volatile boolean running = false;

        @Override
        public void run() {
            running = true;
            while (running) {
                try {
                    ActorTask task = taskPool.take();
                    if (task == null) {
                        continue;
                    }

                    if (task == EXIT_SIGNAL) {
                        break;
                    }

                    TaskLock lock = locks.get(task.getLockKey());
                    if (lock.acquire(task)) {
                        task.run();
                    }
                } catch (Throwable cause) {
                    cause.printStackTrace();
                    continue;
                }
            }
        }

        public synchronized void shutdown() {
            running = false;
        }
    }

    public static final class TaskLock {
        private final ActorLock key;

        private final Actor actor;

        private final Queue<ActorTask> pendPool = new ConcurrentLinkedQueue<ActorTask>();

        /**
         * 当前可以执行的线程锁任务
         */
        private volatile ActorTask currentTask = null;

        private TaskLock(ActorLock key, Actor actor) {
            this.key = key;
            this.actor = actor;
        }

        /**
         * 获取锁资源执行任务，
         * 如果当前队列中有在执行的任务则放入等等队列中等待上一个任务执行完成并释放锁资源
         */
        public synchronized boolean acquire(ActorTask task) {
            if (currentTask == null || currentTask == task) {
                // 当前锁还没被线程获取或者本来就是可执行线程，可以直接执行
                currentTask = task;
                return true;
            }
            // 已经有一个任务拿到锁了，扔到等待队列中
            // 等待任务组内的任务执行完成之后才将未执行的任务重新添加到执行队列中
            pendPool.offer(task);
            return false;
        }

        /**
         * 任务执行完成，释放锁资源，由异步任务手动执行，
         * 释放锁后会从任务等待队列获取先进来的任务再执行
         */
        public synchronized boolean release() {
            boolean complete = false;
            if (pendPool.isEmpty()) {
                actor.removeLock(key);
                complete = true;
            } else {
                // 任务执行完毕，把缓冲队列中未执行的任务重新添加到执行队列中
                // 注意，队列是先进先出，所以未执行的任务可能要比其他任务要慢执行
                currentTask = pendPool.poll();
                actor.addTask(currentTask);
            }
            return complete;
        }
    }
}
