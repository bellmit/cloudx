package cloud.apposs.cachex;

/**
 * 缓存Key包装类
 */
public abstract class AbstractCacheKey<T> implements CacheKey<T> {
    /**
     * 缓存Key值，一般为索引ID
     * 1. 在添加操作中，没添加数据前为空，数据添加后框架自动更新
     * 2. 在查询操作，则需要赋值此key
     */
    protected T primary;

    /**
     * 缓存Key前缀，如果实现{@link #getCacheKey()}则不需要此字段，否则该字段不能为空
     */
    protected String prefix;

    public AbstractCacheKey() {
    }

    public AbstractCacheKey(String prefix) {
        this.prefix = prefix;
    }

    public AbstractCacheKey(T primary, String prefix) {
        this.primary = primary;
        this.prefix = prefix;
    }

    @Override
    public T getPrimary() {
        return primary;
    }

    @Override
    public void setPrimary(T primary) {
        this.primary = primary;
    }

    @Override
    public String getCacheKey() {
        return prefix + primary;
    }

    @Override
    public int getLockIndex() {
        return -1;
    }

    @Override
    public String toString() {
        return getCacheKey();
    }
}
