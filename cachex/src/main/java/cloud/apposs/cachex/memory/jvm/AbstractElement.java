package cloud.apposs.cachex.memory.jvm;

import cloud.apposs.util.StrUtil;

public abstract class AbstractElement implements Element {
	private static final long serialVersionUID = 3802339743879374128L;

	/** 缓存Key */
	protected final String key;
	
	/** 缓存数据 */
	protected final Object value;
    
    /** 缓存命中次数 */
	protected volatile long hitCount = 0L;
    
    /** 缓存的创建时间，主要用于判断缓存是否过期 */
	protected final long creationTime = System.currentTimeMillis();
    
    /** 最近访问时间，包括ADD/GET/UPDATE */
	protected long lastAccessTime = System.currentTimeMillis();
    
    /** 缓存过期时间，单位毫秒，小于等于0为永为过期 */
	protected int expirationTime = -1;
	
	/** 缓存数据字节大小，为大概值 */
	protected int byteSize = 0;

	public AbstractElement(String key, Object value) {
		if (StrUtil.isEmpty(key)) {
			throw new IllegalArgumentException("key");
		}
		if (value == null) {
			throw new IllegalArgumentException("value");
		}
		
		this.key = key;
		this.value = value;
		this.byteSize = doCalculateByteSize();
	}
	
	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Object getValue() {
		return getValue(true);
	}
	
	@Override
	public Object getValue(boolean update) {
		if (update) {
			doUpdateStatus();
		}
		return value;
	}
	
	@Override
	public int getByteSize() {
		return byteSize;
	}

	@Override
	public final long getHitCount() {
        return hitCount;
    }

	@Override
	public long getCreationTime() {
		return creationTime;
	}

	@Override
	public long getLastAccessTime() {
		return lastAccessTime;
	}
	
	@Override
	public int getExpirationTime() {
		return expirationTime;
	}

	@Override
	public int setExpirationTime(int expirationTime) {
		int expire = this.expirationTime;
		this.expirationTime = expirationTime;
		return expire;
	}
	
	@Override
	public boolean isExpired() {
		if (expirationTime < 0) {
			return false;
		}
		long liveTime = System.currentTimeMillis() - creationTime;
        return liveTime > expirationTime;
	}
	
	/**
	 * 计算当前缓存数据的大小
	 */
	public abstract int doCalculateByteSize();

	/**
	 * 每次访问缓存时的状态更新
	 */
	protected void doUpdateStatus() {
		hitCount++;
		lastAccessTime = System.currentTimeMillis();
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
        info.append("[Key=").append(key);
        info.append(", Value=").append(value);
        info.append(", HitCount=").append(hitCount);
        info.append(", ExpirationTime=").append(expirationTime);
        info.append(", CreationTime=").append(creationTime);
        info.append(", LastAccessTime=").append(lastAccessTime);
        info.append("]");
        return info.toString();
	}
}
