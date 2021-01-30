package cloud.apposs.cachex;

/**
 * 缓存信息统计服务，包括命名率，平时耗时等
 */
public final class CacheXStatistics {
	/** 命中缓存的次数 */
	private long hitCount = 0L;
	
	/** 没命中缓存的次数 */
	private long missCount = 0L;
	
	public long getHitCount() {
		return hitCount;
	}

	public void addHitCount() {
		this.hitCount++;
	}

	public long getMissCount() {
		return missCount;
	}
	
	public void addMissCount() {
		this.missCount++;
	}
	
	/**
	 * 获取缓存命中率
	 */
	public int getHitRatio() {
		return (int) ((hitCount * 100) / (hitCount + missCount));
	}
}
