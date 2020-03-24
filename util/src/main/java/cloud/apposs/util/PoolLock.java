package cloud.apposs.util;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PoolLock {
    public static final int DEFAULT_LOCK_LENGTH = 4096;
    public static final int DEFAULT_MAX_LOCK_LENGTH = Integer.MAX_VALUE;
    /** 锁超时时间，默认1分钟 */
    public static final int DEFAULT_LOCK_TIMEOUT = 60 * 1000;

    /** 空闲锁队列 */
    private final Queue<ReentrantReadWriteLock> locks = new ConcurrentLinkedQueue<ReentrantReadWriteLock>();

    /** 正在使用的锁 */
    private final Map<Integer, ReentrantReadWriteLock> wokers = new ConcurrentHashMap<Integer, ReentrantReadWriteLock>();

    /** 缓存池同步锁 */
    private final ReentrantLock mainLock = new ReentrantLock();

    /** 取出可用锁的阻塞锁 */
    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition lockAvail = takeLock.newCondition();

    /** 正在使用的锁数量 */
    private volatile int numUse = 0;

    private volatile int coreLockLength = DEFAULT_LOCK_LENGTH;
    private volatile int maxLockLength = DEFAULT_MAX_LOCK_LENGTH;

    public PoolLock() {
        this(DEFAULT_LOCK_LENGTH, DEFAULT_MAX_LOCK_LENGTH);
    }

    public PoolLock(int coreLockLength, int maxLockLength) {
        this.coreLockLength = coreLockLength;
        this.maxLockLength = maxLockLength;
        // 初始化锁
        for (int i = 0; i < coreLockLength; i++) {
            doAddLock();
        }
    }

    /**
     * 加读锁，默认锁超时60秒
     *
     * @param  index 锁索引位置
     * @return 加锁成功返回true
     */
    public final boolean readLock(int index) {
        return readLock(index, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 加读锁
     *
     * @param  index   锁索引位置
     * @param  timeout 锁超时时间
     * @return 加锁成功返回true
     */
    public final boolean readLock(int index, int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout");
        }
        ReentrantReadWriteLock lock = wokers.get(index);
        if (lock == null) {
            lock = doGetLock(timeout);
        }

        try {
            return lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 获取空闲锁池中可用锁，如果没有可用锁并且资源未耗竭则自动创建并返回
     * 否则阻塞等待直到有其他请求锁已经释放时再获取
     *
     * @param timeout 等待超时时间
     * @return 空闲锁池中可用锁
     */
    private ReentrantReadWriteLock doGetLock(long timeout) {
        checkTasks();
        ReentrantReadWriteLock lock = null;
        final ReentrantLock takeLock = this.takeLock;
        try {
            takeLock.lockInterruptibly();
            while (locks.size() == 0) {
                if (timeout > 0) {
                    lockAvail.await(timeout, TimeUnit.MILLISECONDS);
                    break;// 到达超时时间，无论空闲队列有没有空闲线程都退出
                } else {
                    lockAvail.await();
                }
            }
            lock = locks.size() > 0 ? locks.poll() : null;
        } catch (InterruptedException e) {
            lockAvail.signal();
        } finally {
            takeLock.unlock();
        }
        return lock;
    }

    /**
     * 判断可用锁是否已经耗尽
     */
    public boolean isExhausted() {
        // 只有当空闲线程为0并且忙碌线程数大于最大线程数时判断为资源耗竭
        return locks.isEmpty() && (numUse >= maxLockLength);
    }

    /**
     * 创建添加可用锁，为系统内部调用
     */
    private void doAddLock() {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        locks.add(lock);
    }

    /**
     * 检查缓存锁池，当可用锁未耗竭并且空闲锁时小于核心锁数量时，系统自动创建并添加空闲锁到锁池中
     */
    private void checkTasks() {
        if (!isExhausted() && locks.size() < maxLockLength) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (locks.isEmpty() || locks.size() < maxLockLength ) {
                    doAddLock();
                }
            } finally {
                mainLock.unlock();
            }
        }
    }
}
