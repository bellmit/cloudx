package cloud.apposs.cachex.memory.redis.codis;

import cloud.apposs.cachex.CacheXConfig.RedisConfig;

public final class RedisWatcherFactory {
	public static RedisWatcher getRedisWatcher(RedisConfig config, RedisPool pool) {
		int watcherType = config.getWatcherType();
		if (watcherType == RedisConfig.REDIS_WATCHER_ZOOKEEPER) {
		} else if (watcherType == RedisConfig.REDIS_WATCHER_QCONF) {
		}
		return new RedisWatcherThread(config, pool);
	}
}
