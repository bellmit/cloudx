package cloud.apposs.cachex.memory.jvm;

/**
 * JVM缓存操作相关监听服务
 */
public interface JvmCacheListener {
	/**
	 * 添加缓存事件监听
	 * 
	 * @param key   缓存Key
	 * @param value 缓存数据
	 */
	void cachePut(String key, Element value);
	
	/**
	 * 移除缓存事件监听
	 * 
	 * @param key   缓存Key
	 * @param value 缓存数据
	 */
	void cacheRemove(String key, Element value);
	
	/**
	 * JVM缓存过期监听
	 * 
	 * @param key   缓存Key
	 * @param value 缓存数据
	 */
	void cacheExpired(String key, Element value);

	/**
	 * JVM缓存到达最大条数限制或者最大容量限制时触发的内存缓存回收监听
	 * 
	 * @param key   缓存Key
	 * @param value 缓存数据
	 */
	void cacheEvicted(String key, Element value);
}
