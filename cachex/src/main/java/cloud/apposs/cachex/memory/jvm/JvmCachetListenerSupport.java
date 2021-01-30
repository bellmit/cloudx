package cloud.apposs.cachex.memory.jvm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JvmCachetListenerSupport {
	private final List<JvmCacheListener> listeners = new CopyOnWriteArrayList<JvmCacheListener>();
	
	public void add(JvmCacheListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
	
	public void remove(JvmCacheListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

	public void fireCachePut(String key, Element value) {
		for (JvmCacheListener listener : listeners) {
        	listener.cachePut(key, value);
        }
	}

	public void fireCacheRemove(String key, Element value) {
		for (JvmCacheListener listener : listeners) {
        	listener.cacheRemove(key, value);
        }
	}

	public void fireCacheExpired(String key, Element value) {
		for (JvmCacheListener listener : listeners) {
        	listener.cacheExpired(key, value);
        }
	}
	
	public void fireCacheEvicted(String key, Element value) {
		for (JvmCacheListener listener : listeners) {
        	listener.cacheEvicted(key, value);
        }
	}
}
