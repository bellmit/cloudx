package cloud.apposs.cachex.memory.redis.codis;

import java.util.LinkedList;
import java.util.List;

public final class RedisConnectionListenerSupport {
	private final List<RedisConnectionListener> listeners = new LinkedList<RedisConnectionListener>();
	
	public void add(RedisConnectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
	
	public void remove(RedisConnectionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

	public void fireConnectionInValid(RedisConnection connection) {
		for (RedisConnectionListener listener : listeners) {
        	listener.connectionInvalid(connection);
        }
	}

	public void fireConnectionCreated(RedisConnection connection) {
		for (RedisConnectionListener listener : listeners) {
        	listener.connectionCreated(connection);
        }
	}

	public void fireConnectionRetrived(RedisConnection connection) {
		for (RedisConnectionListener listener : listeners) {
        	listener.connectionRetrived(connection);
        }
	}
}
