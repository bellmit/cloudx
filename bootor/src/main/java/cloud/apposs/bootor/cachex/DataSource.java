package cloud.apposs.bootor.cachex;

import cloud.apposs.bootor.BootorConfig;
import cloud.apposs.cachex.CacheKey;
import cloud.apposs.cachex.CacheLoader;
import cloud.apposs.cachex.ParamCacheX;
import cloud.apposs.util.Param;

/**
 * 基于数据类型为{@link Param}的数据服务，
 * 之所以多一层封装在于可以在底层添加以下功能而不需要业务关心底层细节，包括
 * 1. 添加DAO操作监控，上报到监控平台
 * 2. 输出慢日志查询
 * 3. 输出缓存命中统计
 */
public class DataSource<K extends CacheKey> extends ParamCacheX<K> {
    public static <K extends CacheKey> DataSource<K> create(BootorConfig config, CacheLoader<K, Param> loader) {
        return new DataSource<>(config, loader);
    }

    public static <K extends CacheKey> DataSource<K> create(BootorConfig config, String reource, CacheLoader<K, Param> loader) {
        return new DataSource<>(config, reource, loader);
    }

    public DataSource(BootorConfig config, CacheLoader<K, Param> loader) {
        super(config.getCacheXConfig(), loader);
    }

    public DataSource(BootorConfig config, String reource, CacheLoader<K, Param> loader) {
        super(config.getCacheXConfig(reource), loader);
    }

    public DataSource(BootorConfig config, CacheLoader<K, Param> loader, int lockLength) {
        super(config.getCacheXConfig(), loader, lockLength);
    }

    public DataSource(BootorConfig config, String reource, CacheLoader<K, Param> loader, int lockLength) {
        super(config.getCacheXConfig(reource), loader, lockLength);
    }
}
