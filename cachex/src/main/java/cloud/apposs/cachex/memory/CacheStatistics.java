package cloud.apposs.cachex.memory;

/**
 * 缓存信息统计服务，包括命中率，占用内存等
 */
public final class CacheStatistics {
    /**
     * 命中缓存的次数，不用锁，允许有数据误差
     */
    private volatile long hitCount = 0L;

    /**
     * 没命中缓存的次数，不用锁，允许有数据误差
     */
    private volatile long missCount = 0L;

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
