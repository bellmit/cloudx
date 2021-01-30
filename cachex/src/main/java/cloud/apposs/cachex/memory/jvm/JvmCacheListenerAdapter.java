package cloud.apposs.cachex.memory.jvm;

public class JvmCacheListenerAdapter implements JvmCacheListener {
	@Override
	public void cachePut(String key, Element value) {
	}

	@Override
	public void cacheRemove(String key, Element value) {
	}
	
	@Override
	public void cacheExpired(String key, Element value) {
	}

	@Override
	public void cacheEvicted(String key, Element value) {
	}
}
