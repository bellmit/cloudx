package cloud.apposs.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 缓存分段锁
 */
public final class CacheLock {
	public static final int DEFAULT_LOCK_LENGTH = 4096;
	/** 锁超时时间，默认1分钟 */
	public static final int DEFAULT_LOCK_TIMEOUT = 60 * 1000;
	
	/** 分段锁长度 */
	private final int length;
	
	/** 分段锁 */
	private final ReentrantReadWriteLock[] locks;

	public CacheLock() {
		this(DEFAULT_LOCK_LENGTH);
	}
	
	public CacheLock(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException("length");
		}
		this.length = length;
		this.locks = new ReentrantReadWriteLock[length];
		for (int i = 0; i < length; ++i) {
			this.locks[i] = new ReentrantReadWriteLock();
		}
	}
	
	/**
	 * 获取锁分段长度
	 */
	public final int getLength() {
		return length;
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
		if (index < 0) {
			index = Math.abs(index);
		}
		int position = position(index);
		try {
			return locks[position].readLock().tryLock(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	/**
	 * 解除读锁
	 */
	public final void readUnlock(int index) {
		if (index < 0) {
			index = Math.abs(index);
		}
		int position = position(index);
		locks[position].readLock().unlock();
	}
	
	/**
	 * 加写锁，默认锁超时60秒
	 * 
	 * @param  index 锁索引位置
	 * @return 加锁成功返回true
	 */
	public final boolean writeLock(int index) {
		return writeLock(index, DEFAULT_LOCK_TIMEOUT);
	}
	
	/**
	 * 加写锁
	 * 
	 * @param  index   锁索引位置
	 * @param  timeout 锁超时时间
	 * @return 加锁成功返回true
	 */
	public final boolean writeLock(int index, int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout");
		}
		if (index < 0) {
			index = Math.abs(index);
		}
		int position = position(index);
		try {
			return locks[position].writeLock().tryLock(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	/**
	 * 解除写锁
	 */
	public final void writeUnlock(int index) {
		if (index < 0) {
			index = Math.abs(index);
		}
		int position = position(index);
		locks[position].writeLock().unlock();
	}
	
	private final int position(int index) {
		return index % locks.length;
	}
}
