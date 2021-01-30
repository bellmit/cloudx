package cloud.apposs.cachex.memory.redis.codis;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.memory.Cache;
import cloud.apposs.cachex.memory.CacheStatistics;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Codis代理Redis集群管理，通过随机获取Codis代理实现对Reids的数据存取
 */
public class CodisCache implements Cache {
    private static final long serialVersionUID = -2980655272329194063L;

    private final CacheXConfig config;

    /**
     * Redis连接池
     */
    private final RedisPool pool;

    /**
     * 缓存统计服务
     */
    private final CacheStatistics statistics = new CacheStatistics();

    private final Random random = new Random();

    public CodisCache(CacheXConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config");
        }
        this.config = config;
        this.pool = new RedisPool(config.getRedisConfig());
    }

    @Override
    public CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean exists(String key) {
        RedisConnection connection = doGetConnection();
        try {
            return connection.exists(key);
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public int expire(String key, int expirationTime) {
        RedisConnection connection = doGetConnection();
        try {
            long expire = connection.ttl(key);
            connection.expire(key, expirationTime / 1000);
            return (int) expire;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public ProtoBuf get(String key) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            byte[] value = connection.get(key.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return ProtoBuf.wrap(value);
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public ProtoBuf getBuffer(String key) {
        return get(key);
    }

    @Override
    public byte[] getBytes(String key) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            byte[] value = connection.get(key.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public String getString(String key) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            String value = connection.get(key);
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseConnection(connection);
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

        RedisConnection connection = doGetConnection();
        try {
            if (compact) {
                value.compact();
            }
            Charset charset = config.getChrset();
            boolean result = connection.set(key.getBytes(charset), value.array());
            doSetExpire(connection, key);
            return result;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean put(String key, String value) {
        if (key == null || value == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            boolean result = connection.set(key, value);
            doSetExpire(connection, key);
            return result;
        } finally {
            doReleaseConnection(connection);
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

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            boolean result = connection.set(key.getBytes(charset), value);
            doSetExpire(connection, key);
            return result;
        } finally {
            doReleaseConnection(connection);
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

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            byte[] value = connection.hget(key.getBytes(charset), field.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return ProtoBuf.wrap(value);
        } finally {
            doReleaseConnection(connection);
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

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            byte[] value = connection.hget(key.getBytes(charset), field.getBytes(charset));
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public String hgetString(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            String value = connection.hget(key, field);
            if (value == null) {
                statistics.addMissCount();
                return null;
            }

            statistics.addHitCount();
            return value;
        } finally {
            doReleaseConnection(connection);
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
    public Map<?, ?> hgetMap(String key, String field, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getMap(schema);
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
    public <T> T hgetObject(String key, String field, Class<T> clazz, ProtoSchema schema) {
        ProtoBuf value = hget(key, field);
        if (value == null) {
            return null;
        }
        return value.getObject(clazz, schema);
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

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            Map<String, ProtoBuf> value = new HashMap<String, ProtoBuf>();
            Map<byte[], byte[]> values = connection.hgetAll(key.getBytes(charset));
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
            doReleaseConnection(connection);
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

        RedisConnection connection = doGetConnection();
        try {
            List<String> value = new LinkedList<String>();
            Map<String, String> values = connection.hgetAll(key);
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
            doReleaseConnection(connection);
        }
    }

    @Override
    public <T> List<T> hgetAllObject(String key, Class<T> clazz, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            List<T> value = new LinkedList<T>();
            Map<byte[], byte[]> values = connection.hgetAll(key.getBytes(charset));
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
            doReleaseConnection(connection);
        }
    }

    @Override
    public List<Param> hgetAllParam(String key, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            List<Param> value = new LinkedList<Param>();
            Map<byte[], byte[]> values = connection.hgetAll(key.getBytes(charset));
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
            doReleaseConnection(connection);
        }
    }

    @Override
    public List<Table<?>> hgetAllTable(String key, ProtoSchema schema) {
        if (key == null) {
            return null;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            List<Table<?>> value = new LinkedList<Table<?>>();
            Map<byte[], byte[]> values = connection.hgetAll(key.getBytes(charset));
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
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean hput(String key, String field, ProtoBuf value, boolean compact) {
        if (key == null || field == null || value == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            long count = connection.hset(key.getBytes(charset), field.getBytes(charset), value.array());
            doSetExpire(connection, key);
            return count >= 0;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean hput(String key, String field, String value) {
        if (key == null || field == null || value == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            long count = connection.hset(key, field, value);
            doSetExpire(connection, key);
            return count >= 0;
        } finally {
            doReleaseConnection(connection);
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
    public boolean hput(String key, String field, byte[] value) {
        if (key == null || field == null || value == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            long count = connection.hset(key.getBytes(charset), field.getBytes(charset), value);
            doSetExpire(connection, key);
            return count >= 0;
        } finally {
            doReleaseConnection(connection);
        }
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

        ProtoBuf buffer = new ProtoBuf();
        buffer.putList(value, schema);
        return hput(key, field, buffer, false);
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

        RedisConnection connection = doGetConnection();
        try {
            Charset charset = config.getChrset();
            boolean success = connection.hmset(key.getBytes(charset), value);
            doSetExpire(connection, key);
            return success;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            return connection.del(key) >= 0;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean remove(String... keys) {
        if (keys == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            return connection.del(keys) >= 0;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean remove(String key, String field) {
        if (key == null || field == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            return connection.hdel(key, field) >= 0;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public boolean remove(String key, String... fields) {
        if (key == null || fields == null) {
            return false;
        }

        RedisConnection connection = doGetConnection();
        try {
            return connection.hdel(key, fields) >= 0;
        } finally {
            doReleaseConnection(connection);
        }
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    /**
     * 设置过期时间
     */
    private void doSetExpire(RedisConnection connection, String key) {
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
            connection.expire(key, expirationTime);
        }
    }

    private RedisConnection doGetConnection() {
        RedisConnection connection = pool.getConnection();
        return connection;
    }

    private void doReleaseConnection(RedisConnection connection) {
        connection.close();
    }
}
