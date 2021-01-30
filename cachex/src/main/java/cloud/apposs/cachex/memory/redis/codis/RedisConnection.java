package cloud.apposs.cachex.memory.redis.codis;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;

import redis.clients.jedis.Jedis;

/**
 * Redis连接包装类
 */
public final class RedisConnection {
	public static final String REDIS_RESPONSE_OK = "OK";
	public static final String PING_RESULT = "PONG";
	
	/** 直接的Redis连接 */
	private final Jedis connection;
	
	private final RedisConnectionPartition partition;
	
	/** 
	 * 当前Connection是否被关闭，即被调用{@link #close()}方法
	 * 此时真正Connection并未被真正关闭，只是逻辑意义上的关闭
	 * 在下次从数据库连接池取出时仍然可用
	 */
	private AtomicBoolean logicallyClosed = new AtomicBoolean(false);

	public RedisConnection(RedisConnectionPartition partition, 
			String host, int port, int connectTimeout, int recvTimeout) {
		this.connection = new Jedis(host, port, connectTimeout, recvTimeout);
		this.connection.connect();
		this.partition = partition;
	}
	
	public boolean exists(String key) {
		return connection.exists(key);
	}
	
	public void expire(String key, int expirationTime) {
		connection.expire(key, expirationTime);
	}
	
	public long ttl(String key) {
		return connection.ttl(key);
	}
	
	public byte[] get(byte[] key) {
		return connection.get(key);
	}
	
	public String get(String key) {
		return connection.get(key);
	}
	
	public boolean set(byte[] key, byte[] value) {
		return REDIS_RESPONSE_OK.equals(connection.set(key, value));
	}
	
	public boolean set(String key, String value) {
		return REDIS_RESPONSE_OK.equals(connection.set(key, value));
	}
	
	public byte[] hget(byte[] key, byte[] field) {
		return connection.hget(key, field);
	}
	
	public String hget(String key, String field) {
		return connection.hget(key, field);
	}
	
	public long hset(byte[] key, byte[] field, byte[] value) {
		return connection.hset(key, field, value);
	}
	
	public long hset(String key, String field, String value) {
		return connection.hset(key, field, value);
	}
	
	public boolean hmset(byte[] key, Map<byte[], byte[]> value) {
		return REDIS_RESPONSE_OK.equals(connection.hmset(key, value));
	}
	
	public Map<byte[], byte[]> hgetAll(byte[] key) {
		return connection.hgetAll(key);
	}
	
	public Map<String, String> hgetAll(String key) {
		return connection.hgetAll(key);
	}

	public Jedis getRawConnection() {
		return connection;
	}
	
	public boolean isConnected() {
		return connection.isConnected();
	}
	
	public boolean isClosed() {
		return logicallyClosed.get();
	}

	public void close() {
		if (!logicallyClosed.get()) {
			logicallyClosed.set(true);
			partition.retriveConnection(this);
			if (partition.getConfig().isPoolOperationWatch()) {
				partition.removeConnectionStackTrace(this);
			}
		}
	}
	
	protected void internalClose() {
		connection.disconnect();
	}
	
	/**
	 * 激活Connection连接，在从数据库连接池取出Connection连接时调用此方法
	 */
	protected void active() {
		this.logicallyClosed.set(false);
	}
	
	/**
	 * 检查Redis连接是否正常
	 * 
	 * @param  server 检测的Redis服务
	 * @param  connectTimeout 连接超时时间
	 * @param  recvTimeout 读取超时时间
	 * @return 连接正常返回true
	 */
	public static boolean checkRedisAlive(RedisServer server, 
			int connectTimeout, int recvTimeout) {
		if (server == null || connectTimeout <= 0 || recvTimeout <= 0) {
			return false;
		}
		
		Jedis jedis = null;
		try {
			String host = server.getHost();
			int port = server.getPort();
			jedis = new Jedis(host, port, connectTimeout, recvTimeout);
			String result = jedis.ping();
			if (PING_RESULT.equals(result)) {
				jedis.disconnect();
				return true;
			}
		} catch (Throwable t) {
			return false;
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
		return false;
	}
	
	public long del(String key) {
		return connection.del(key);
	}
	
	public long del(String[] keys) {
		return connection.del(keys);
	}

	public long hdel(String key, String field) {
		return connection.hdel(key, field);
	}

	public long hdel(String key, String... fields) {
		return connection.hdel(key, fields);
	}
}
