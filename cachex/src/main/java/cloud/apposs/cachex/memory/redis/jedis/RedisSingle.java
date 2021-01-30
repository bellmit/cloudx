package cloud.apposs.cachex.memory.redis.jedis;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;
import cloud.apposs.cachex.memory.Cache;
import cloud.apposs.cachex.memory.CacheStatistics;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Redis单机缓存管理
 */
public class RedisSingle implements Cache {
    private static final long serialVersionUID = 7211934533896702648L;

    public static final String REDIS_RESPONSE_OK = "OK";

    private final CacheXConfig config;

    /**
     * Jedis原生连接池
     */
    private final JedisPool jedisPool;

    /**
     * Redis没有获取缓存总数的API，只能自己统计
     */
    private volatile int size;

    /**
     * 缓存统计服务
     */
    private final CacheStatistics statistics = new CacheStatistics();

    private final Random random = new Random();

    public RedisSingle(CacheXConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config");
        }
        RedisConfig redisConfig = config.getRedisConfig();
        List<RedisServer> serverList = redisConfig.getServerList();
        if (serverList == null || serverList.isEmpty()) {
            throw new IllegalArgumentException("Redis Server List Not Configed");
        }

        this.config = config;
        RedisServer server = serverList.get(0);
        JedisPoolConfig jedisConfig = new JedisPoolConfig();
        jedisConfig.setMaxIdle(redisConfig.getMaxConnections());
        jedisConfig.setMinIdle(redisConfig.getMinConnections());
        jedisConfig.setTestOnBorrow(redisConfig.isTestConnectionOnCheckout());
        jedisConfig.setTestOnReturn(redisConfig.isTestConnectionOnCheckin());
        this.jedisPool = new JedisPool(jedisConfig, server.getHost(), server.getPort());
    }

    @Override
    public CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean exists(String key) {
        Jedis jedis = doGetJedis();
        try {
            return jedis.exists(key);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public int expire(String key, int expirationTime) {
        Jedis jedis = doGetJedis();
        try {
            long expire = jedis.ttl(key);
            jedis.expire(key, expirationTime / 1000);
            return (int) expire;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public ProtoBuf get(String key) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            byte[] value = jedis.get(key.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return ProtoBuf.wrap(value);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public ProtoBuf getBuffer(String key) {
        return get(key);
    }

    @Override
    public String getString(String key) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            String value = jedis.get(key);
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public Integer getInt(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value);
    }

    @Override
    public Long getLong(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Override
    public Short getShort(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Short.valueOf(value);
    }

    @Override
    public Float getFloat(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Float.valueOf(value);
    }

    @Override
    public Double getDouble(String key) {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Double.valueOf(value);
    }

    @Override
    public byte[] getBytes(String key) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            byte[] value = jedis.get(key.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public Map<?, ?> getMap(String key, ProtoSchema schema) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        Map<?, ?> value = buffer.getMap(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public List<?> getList(String key, ProtoSchema schema) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        List<?> value = buffer.getList(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz, ProtoSchema schema) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        T value = buffer.getObject(clazz, schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Param getParam(String key, ProtoSchema schema) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        Param value = buffer.getParam(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Table<?> getTable(String key, ProtoSchema schema) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        Table<?> value = buffer.getTable(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public boolean put(String key, ProtoBuf value) {
        return put(key, value, false);
    }

    @Override
    public boolean put(String key, ProtoBuf value, boolean compact) {
        if (key == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            if (compact) {
                value.compact();
            }
            Charset charset = config.getChrset();
            String result = jedis.set(key.getBytes(charset), value.array());
            doSetExpire(jedis, key);
            return REDIS_RESPONSE_OK.equals(result);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean put(String key, String value) {
        if (key == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            String result = jedis.set(key, value);
            doSetExpire(jedis, key);
            return REDIS_RESPONSE_OK.equals(result);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean put(String key, int value) {
        return put(key, String.valueOf(value));
    }

    @Override
    public boolean put(String key, long value) {
        return put(key, String.valueOf(value));
    }

    @Override
    public boolean put(String key, short value) {
        return put(key, String.valueOf(value));
    }

    @Override
    public boolean put(String key, double value) {
        return put(key, String.valueOf(value));
    }

    @Override
    public boolean put(String key, float value) {
        return put(key, String.valueOf(value));
    }

    @Override
    public boolean put(String key, byte[] value) {
        if (key == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            String result = jedis.set(key.getBytes(charset), value);
            doSetExpire(jedis, key);
            return REDIS_RESPONSE_OK.equals(result);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean put(String key, Object value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf();
        buffer.putObject(value, schema);
        return put(key, buffer, false);
    }

    /**
     * 添加Map缓存，
     * 注意因为是整个Map数据进行序列化并存储，实际上并没有利用到Redis的Map存储特性，
     * 如果想利用Redis Map值存储特性，建议采用hput进行二级缓存存储
     */
    @Override
    public boolean put(String key, Map<?, ?> value, ProtoSchema schema) {
        ProtoBuf buffer = new ProtoBuf();
        buffer.putMap(value, schema);
        return put(key, buffer, false);
    }

    @Override
    public boolean put(String key, List<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putList(value, schema);
        return put(key, buffer, false);
    }

    @Override
    public boolean put(String key, Param value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putParam(value, schema);
        return put(key, buffer, false);
    }

    @Override
    public boolean put(String key, Table<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putTable(value, schema);
        return put(key, buffer, false);
    }

    @Override
    public ProtoBuf hget(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            byte[] value = jedis.hget(key.getBytes(charset), field.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return ProtoBuf.wrap(value);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public ProtoBuf hgetBuffer(String key, String field) {
        return hget(key, field);
    }

    @Override
    public byte[] hgetBytes(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            return jedis.hget(key.getBytes(charset), field.getBytes(charset));
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public <T> T hgetObject(String key, String field, Class<T> clazz, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getObject(clazz, schema);
    }

    @Override
    public String hgetString(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            String value = jedis.hget(key, field);
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public Integer hgetInt(String key, String field) {
        String value = hgetString(key, field);
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value);
    }

    @Override
    public Long hgetLong(String key, String field) {
        String value = hgetString(key, field);
        if (value == null) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Override
    public Short hgetShort(String key, String field) {
        String value = hgetString(key, field);
        if (value == null) {
            return null;
        }
        return Short.valueOf(value);
    }

    @Override
    public Double hgetDouble(String key, String field) {
        String value = hgetString(key, field);
        if (value == null) {
            return null;
        }
        return Double.valueOf(value);
    }

    @Override
    public Float hgetFloat(String key, String field) {
        String value = hgetString(key, field);
        if (value == null) {
            return null;
        }
        return Float.valueOf(value);
    }

    @Override
    public List<?> hgetList(String key, String field, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getList(schema);
    }

    @Override
    public Map<?, ?> hgetMap(String key, String field, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getMap(schema);
    }

    @Override
    public Param hgetParam(String key, String field, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getParam(schema);
    }

    @Override
    public Table<?> hgetTable(String key, String field, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getTable(schema);
    }

    @Override
    public Map<String, ProtoBuf> hgetAll(String key) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            Map<String, ProtoBuf> value = new HashMap<String, ProtoBuf>();
            Map<byte[], byte[]> values = jedis.hgetAll(key.getBytes(charset));
            if (values == null) {
                statistics.addMissCount();
                return null;
            }

            for (Entry<byte[], byte[]> entry : values.entrySet()) {
                String mapKey = new String(entry.getKey(), charset);
                byte[] mapValue = entry.getValue();
                ProtoBuf buffer = ProtoBuf.wrap(mapValue);
                value.put(mapKey, buffer);
            }
            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public Map<String, ProtoBuf> hgetAllBuffer(String key) {
        return hgetAll(key);
    }

    @Override
    public List<String> hgetAllString(String key) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            List<String> value = new LinkedList<String>();
            Map<String, String> values = jedis.hgetAll(key);
            if (values == null) {
                statistics.addMissCount();
                return null;
            }

            for (String data : values.values()) {
                value.add(data);
            }
            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public <T> List<T> hgetAllObject(String key, Class<T> clazz, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            List<T> value = new LinkedList<T>();
            Map<byte[], byte[]> values = jedis.hgetAll(key.getBytes(charset));
            if (values == null) {
                statistics.addMissCount();
                return null;
            }

            for (byte[] data : values.values()) {
                ProtoBuf buffer = ProtoBuf.wrap(data);
                value.add((T) buffer.getObject(clazz, schema));
            }
            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public List<Param> hgetAllParam(String key, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            List<Param> value = new LinkedList<Param>();
            Map<byte[], byte[]> values = jedis.hgetAll(key.getBytes(charset));
            if (values == null) {
                statistics.addMissCount();
                return null;
            }

            for (byte[] data : values.values()) {
                ProtoBuf buffer = ProtoBuf.wrap(data);
                value.add(buffer.getParam(schema));
            }
            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public List<Table<?>> hgetAllTable(String key, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            List<Table<?>> value = new LinkedList<Table<?>>();
            Map<byte[], byte[]> values = jedis.hgetAll(key.getBytes(charset));
            if (values == null) {
                statistics.addMissCount();
                return null;
            }

            for (byte[] data : values.values()) {
                ProtoBuf buffer = ProtoBuf.wrap(data);
                value.add(buffer.getTable(schema));
            }
            statistics.addHitCount();
            return value;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean hput(String key, String field, ProtoBuf value, boolean compact) {
        if (key == null || field == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            long count = jedis.hset(key.getBytes(charset), field.getBytes(charset), value.array());
            doSetExpire(jedis, key);
            return count >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean hput(String key, String field, byte[] value) {
        if (key == null || field == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            long count = jedis.hset(key.getBytes(charset), field.getBytes(charset), value);
            doSetExpire(jedis, key);
            return count >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean hput(String key, String field, String value) {
        if (key == null || field == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            long count = jedis.hset(key, field, value);
            doSetExpire(jedis, key);
            return count >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean hput(String key, String field, int value) {
        return hput(key, field, String.valueOf(value));
    }

    @Override
    public boolean hput(String key, String field, long value) {
        return hput(key, field, String.valueOf(value));
    }

    @Override
    public boolean hput(String key, String field, short value) {
        return hput(key, field, String.valueOf(value));
    }

    @Override
    public boolean hput(String key, String field, double value) {
        return hput(key, field, String.valueOf(value));
    }

    @Override
    public boolean hput(String key, String field, float value) {
        return hput(key, field, String.valueOf(value));
    }

    @Override
    public boolean hput(String key, String field, Object value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf();
        buffer.putObject(value, schema);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, Map<?, ?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf();
        buffer.putMap(value, schema);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, List<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putList(value, schema);
        return hput(key, field, buffer, true);
    }

    @Override
    public boolean hput(String key, String field, Param value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf();
        buffer.putParam(value, schema);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, Table<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf();
        buffer.putTable(value, schema);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hmput(String key, Map<byte[], byte[]> value) {
        if (key == null || value == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            Charset charset = config.getChrset();
            String result = jedis.hmset(key.getBytes(charset), value);
            doSetExpire(jedis, key);
            return REDIS_RESPONSE_OK.equals(result);
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            return jedis.del(key) >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean remove(String... keys) {
        if (keys == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            return jedis.del(keys) >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean remove(String key, String field) {
        if (key == null || field == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            return jedis.hdel(key, field) >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public boolean remove(String key, String... fields) {
        if (key == null || fields == null) {
            return false;
        }

        Jedis jedis = doGetJedis();
        try {
            return jedis.hdel(key, fields) >= 0;
        } finally {
            doReleaseJedis(jedis);
        }
    }

    @Override
    public synchronized void shutdown() {
        jedisPool.close();
    }

    /**
     * 设置过期时间
     */
    private void doSetExpire(Jedis jedis, String key) {
        RedisConfig redisConfig = config.getRedisConfig();
        int expirationTime = redisConfig.getExpirationTime();
        if (redisConfig.isExpirationTimeRandom()) {
            // 设置过期时间随机，避免同一时间有大量缓存过期导致回缓压力大
            int timeMin = redisConfig.getExpirationTimeRandomMin();
            int timeMax = redisConfig.getExpirationTimeRandomMax();
            expirationTime = random.nextInt(timeMax - timeMin) + timeMin;
        }
        expirationTime = expirationTime / 1000;
        if (expirationTime > 0) {
            jedis.expire(key, expirationTime);
        }
    }

    private Jedis doGetJedis() {
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

    private void doReleaseJedis(Jedis jedis) {
        jedis.close();
    }
}
