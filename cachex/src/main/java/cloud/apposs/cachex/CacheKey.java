package cloud.apposs.cachex;

/**
 * 缓存Key包装类，示例：AID+SID+BID
 */
public interface CacheKey<T> {
    /**
     * 获取缓存Key，
     * 主要用于缓存Key的设置，通过缓存Key获取缓存数据等，
     * 在select/query等复杂查询中可以允许为空，为空则代表不通过缓存存取数据，因为复杂查询本身缓存命中率就不高，没必要缓存
     */
    String getCacheKey();

    /**
     * 是否采用锁分段技术来加锁保定服务操作原子性，
     * 默认为-1，即缓存、数据库更新不采用锁
     *
     * @return 锁分段索引位置，必须大于0，小于0则不采用锁
     */
    int getLockIndex();

    /**
     * 获取Key唯一主键，示例：AID/ROWKEY
     */
    T getPrimary();

    /**
     * 设置Key唯一主键，
     * 主要服务于一开始数据插入时可能不知道主键数值，需要底层存储完数据后再设置
     */
    void setPrimary(T primary);
}
