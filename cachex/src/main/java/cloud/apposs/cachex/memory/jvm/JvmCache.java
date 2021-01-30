package cloud.apposs.cachex.memory.jvm;

import cloud.apposs.cachex.CacheLock;
import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.JvmConfig;
import cloud.apposs.cachex.memory.Cache;
import cloud.apposs.cachex.memory.CacheStatistics;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.Table;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JVM缓存管理
 */
public class JvmCache implements Cache {
    private static final long serialVersionUID = 8538614539903945002L;

    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    private static final int DEFAULT_SAMPLE_SIZE = 30;
    private static final int MAX_EVICTION_RATIO = 5;

    public static final int DEFAULT_LOCK_LENGTH = 4096;

    /**
     * JVM缓存相关配置
     */
    private final CacheXConfig config;

    /**
     * 一级缓存数据，存储结构为Key->Value
     */
    private final Map<String, Element0> cache0;

    /**
     * 二级缓存数据，存储结构为Key->Key->Value
     */
    private final Map<String, Element1> cache1;

    /**
     * 缓存监听服务
     */
    private final JvmCachetListenerSupport listeners = new JvmCachetListenerSupport();

    /**
     * 缓存过期服务
     */
    private final JvmCacheExpirer expirer;
    private final Random random = new Random();

    /**
     * 缓存回收策略，有LFU/LRU等回收策略，如果不设置则默认随机获取某些缓存进行回收处理
     */
    private final CacheEvictionPolicy policy;

    /**
     * 缓存占用内存大小，单位字节(Byte)
     */
    private final AtomicLong byteSize = new AtomicLong(0L);

    /**
     * 缓存超过上限时的处理服务
     */
    private CacheEvictor cacheEvitor;

    /**
     * 缓存统计服务
     */
    private final CacheStatistics statistics = new CacheStatistics();

    private final CacheLock lock;

    public JvmCache(CacheXConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config");
        }
        this.config = config;
        JvmConfig jvmConfig = config.getJvmConfig();
        int concurrencyLevel = jvmConfig.getConcurrencyLevel();
        this.cache0 = new ConcurrentHashMap<String, Element0>(
                DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, concurrencyLevel);
        this.cache1 = new ConcurrentHashMap<String, Element1>(
                DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, concurrencyLevel);
        String evitionPolicy = jvmConfig.getEvictionPolicy();
        this.policy = CacheEvictionPolicyStrategy.getCachePolicy(evitionPolicy);
        // 初始化缓存回收服务
        int maxElements = jvmConfig.getMaxElements();
        long maxMemory = jvmConfig.getMaxMemory();
        if (maxElements > 0 && maxMemory > 0) {
            throw new IllegalArgumentException("Cache 'MaxMemory' is not compatible with 'MaxElements'");
        }
        if (maxElements > 0) {
            cacheEvitor = new MaxElementEvictor();
        } else if (maxMemory > 0) {
            cacheEvitor = new MaxMemoryEvictor();
        }
        // 初始化锁服务
        lock = new CacheLock(DEFAULT_LOCK_LENGTH);
        // 启动缓存过期监听服务
        int expireCheckInterval = jvmConfig.getExpireCheckInterval();
        this.expirer = new JvmCacheExpirer(expireCheckInterval, this);
        this.expirer.start();
    }

    public Map<String, Element0> getCache0() {
        return cache0;
    }

    public Map<String, Element1> getCache1() {
        return cache1;
    }

    public void addListener(JvmCacheListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JvmCacheListener listener) {
        listeners.remove(listener);
    }

    @Override
    public CacheStatistics getStatistics() {
        return statistics;
    }

    /**
     * 获取缓存占数据条数
     */
    @Override
    public int size() {
        return cache0.size() + cache1.size();
    }

    /**
     * 获取缓存占用内存大小，单位字节（Byte）
     */
    public long getByteSize() {
        return byteSize.get();
    }

    @Override
    public boolean exists(String key) {
        return cache0.containsKey(key) || cache1.containsKey(key);
    }

    @Override
    public int expire(String key, int expirationTime) {
        if (cache0.containsKey(key)) {
            return cache0.get(key).setExpirationTime(expirationTime);
        } else if (cache1.containsKey(key)) {
            return cache1.get(key).setExpirationTime(expirationTime);
        }
        return -1;
    }

    public CacheEvictionPolicy getPolicy() {
        return policy;
    }

    @Override
    public ProtoBuf get(String key) {
        if (key == null) {
            return null;
        }

        Element0 element = cache0.get(key);
        if (element == null) {
            statistics.addMissCount();
            return null;
        }

        // 惰性检查缓存是否已经过期了
        if (element.isExpired()) {
            removeExpired(element);
            return null;
        }
        statistics.addHitCount();
        return element.getValue();
    }

    @Override
    public ProtoBuf getBuffer(String key) {
        return get(key);
    }

    @Override
    public String getString(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        String value = buffer.getString();
        buffer.rewind();
        return value;
    }

    @Override
    public Integer getInt(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        int value = buffer.getInt();
        buffer.rewind();
        return value;
    }

    @Override
    public Long getLong(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        long value = buffer.getLong();
        buffer.rewind();
        return value;
    }

    @Override
    public Short getShort(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        short value = buffer.getShort();
        buffer.rewind();
        return value;
    }

    @Override
    public Float getFloat(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        float value = buffer.getFloat();
        buffer.rewind();
        return value;
    }

    @Override
    public Double getDouble(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        double value = buffer.getDouble();
        buffer.rewind();
        return value;
    }

    @Override
    public byte[] getBytes(String key) {
        ProtoBuf buffer = get(key);
        if (buffer == null) {
            return null;
        }
        byte[] value = buffer.array();
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

        if (compact) {
            value.compact();
        }
        Element0 element = new Element0(key, value);
        int elementByteSize = element.getByteSize();
        // 检查是否超过缓存配置上限，超过则触发回收策略
        if (cacheEvitor != null) {
            cacheEvitor.checkOverLimit(element, elementByteSize);
        }
        // 触发监听服务
        listeners.fireCachePut(key, element);
        // 递增统计缓存内存占用
        doAddByteSize(elementByteSize);
        // 设置过期时间
        JvmConfig jvmConfig = config.getJvmConfig();
        int expirationTime = jvmConfig.getExpirationTime();
        if (jvmConfig.isExpirationTimeRandom()) {
            // 设置过期时间随机，避免同一时间有大量缓存过期导致回缓压力大
            int timeMin = jvmConfig.getExpirationTimeRandomMin();
            int timeMax = jvmConfig.getExpirationTimeRandomMax();
            expirationTime = random.nextInt(timeMax - timeMin) + timeMin;
        }
        element.setExpirationTime(expirationTime);
        cache0.put(key, element);
        return true;
    }

    @Override
    public boolean put(String key, String value) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putString(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, int value) {
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putInt(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, long value) {
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putLong(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, short value) {
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putShort(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, double value) {
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putDouble(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, float value) {
        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putFloat(value);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, byte[] value) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = ProtoBuf.wrap(value);
        return put(key, buffer, false);
    }

    @Override
    public boolean put(String key, Object value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putObject(value, schema);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, Map<?, ?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putMap(value, schema);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, List<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putList(value, schema);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, Param value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putParam(value, schema);
        return put(key, buffer, true);
    }

    @Override
    public boolean put(String key, Table<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putTable(value, schema);
        return put(key, buffer, true);
    }

    @Override
    public ProtoBuf hget(String key, String field) {
        if (key == null || field == null) {
            return null;
        }

        Element1 element = cache1.get(key);
        if (element == null) {
            statistics.addMissCount();
            return null;
        }

        // 惰性检查缓存是否已经过期了
        if (element.isExpired()) {
            removeExpired(element);
            return null;
        }
        statistics.addHitCount();
        return element.getValue().get(field);
    }

    @Override
    public ProtoBuf hgetBuffer(String key, String field) {
        return hget(key, field);
    }

    @Override
    public String hgetString(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        String value = buffer.getString();
        buffer.rewind();
        return value;
    }

    @Override
    public Integer hgetInt(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        int value = buffer.getInt();
        buffer.rewind();
        return value;
    }

    @Override
    public Long hgetLong(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        long value = buffer.getLong();
        buffer.rewind();
        return value;
    }

    @Override
    public Short hgetShort(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        short value = buffer.getShort();
        buffer.rewind();
        return value;
    }

    @Override
    public Double hgetDouble(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        double value = buffer.getDouble();
        buffer.rewind();
        return value;
    }

    @Override
    public Float hgetFloat(String key, String field) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        float value = buffer.getFloat();
        buffer.rewind();
        return value;
    }

    @Override
    public byte[] hgetBytes(String key, String field) {
        return hget(key, field).array();
    }

    @Override
    public <T> T hgetObject(String key, String field, Class<T> clazz, ProtoSchema schema) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        T value = buffer.getObject(clazz, schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Map<?, ?> hgetMap(String key, String field, ProtoSchema schema) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        Map<?, ?> value = buffer.getMap(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public List<?> hgetList(String key, String field, ProtoSchema schema) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        List<?> value = buffer.getList(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Param hgetParam(String key, String field, ProtoSchema schema) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        Param value = buffer.getParam(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Table<?> hgetTable(String key, String field, ProtoSchema schema) {
        ProtoBuf buffer = hget(key, field);
        if (buffer == null) {
            return null;
        }
        Table<?> value = buffer.getTable(schema);
        buffer.rewind();
        return value;
    }

    @Override
    public Map<String, ProtoBuf> hgetAll(String key) {
        if (key == null) {
            return null;
        }

        Element1 element = cache1.get(key);
        if (element == null) {
            statistics.addMissCount();
            return null;
        }
        // 惰性检查缓存是否已经过期了
        if (element.isExpired()) {
            removeExpired(element);
            return null;
        }
        statistics.addHitCount();
        return element.getValue();
    }

    @Override
    public Map<String, ProtoBuf> hgetAllBuffer(String key) {
        return hgetAll(key);
    }

    @Override
    public List<String> hgetAllString(String key) {
        Map<String, ProtoBuf> all = hgetAll(key);
        if (all == null) {
            return null;
        }
        List<String> infoList = new LinkedList<String>();
        for (ProtoBuf value : all.values()) {
            String info = value.getString();
            value.rewind();
            infoList.add(info);
        }
        return infoList;
    }

    @Override
    public <T> List<T> hgetAllObject(String key, Class<T> clazz, ProtoSchema schema) {
        Map<String, ProtoBuf> all = hgetAll(key);
        if (all == null) {
            return null;
        }
        List<T> infoList = new LinkedList<T>();
        for (ProtoBuf value : all.values()) {
            T info = value.getObject(clazz, schema);
            value.rewind();
            infoList.add(info);
        }
        return infoList;
    }

    @Override
    public List<Param> hgetAllParam(String key, ProtoSchema schema) {
        Map<String, ProtoBuf> all = hgetAll(key);
        if (all == null) {
            return null;
        }
        List<Param> infoList = new LinkedList<Param>();
        for (ProtoBuf value : all.values()) {
            Param info = value.getParam(schema);
            value.rewind();
            infoList.add(info);
        }
        return infoList;
    }

    @Override
    public List<Table<?>> hgetAllTable(String key, ProtoSchema schema) {
        Map<String, ProtoBuf> all = hgetAll(key);
        if (all == null) {
            return null;
        }
        List<Table<?>> infoList = new LinkedList<Table<?>>();
        for (ProtoBuf value : all.values()) {
            Table<?> info = value.getTable(schema);
            value.rewind();
            infoList.add(info);
        }
        return infoList;
    }

    @Override
    public boolean hput(String key, String field, ProtoBuf value, boolean compact) {
        if (key == null || field == null || value == null) {
            return false;
        }

        if (compact) {
            value.compact();
        }
        Element1 element = cache1.get(key);
        if (element == null) {
            // 设置过期时间
            JvmConfig jvmConfig = config.getJvmConfig();
            int expirationTime = jvmConfig.getExpirationTime();
            if (jvmConfig.isExpirationTimeRandom()) {
                // 设置过期时间随机，避免同一时间有大量缓存过期导致回缓压力大
                int timeMin = jvmConfig.getExpirationTimeRandomMin();
                int timeMax = jvmConfig.getExpirationTimeRandomMax();
                expirationTime = random.nextInt(timeMax - timeMin) + timeMin;
            }
            // 需要作加锁操作避免多线程执行下同一个KEY的数据覆盖
            try {
                lock.writeLock(key.hashCode());
                // 双重检查
                if (!cache1.containsKey(key)) {
                    element = new Element1(key, new ConcurrentHashMap<String, byte[]>());
                    element.setExpirationTime(expirationTime);
                    doAddByteSize(element.getByteSize());
                    cache1.put(key, element);
                }
            } finally {
                lock.writeUnlock(key.hashCode());
            }
        }
        // 计算该缓存数据的大概字节大小
        int elementByteSize = doCalcuateElement1ValueSize(key, field, value);
        // 检查是否超过缓存配置上限，超过则触发回收策略
        if (cacheEvitor != null) {
            cacheEvitor.checkOverLimit(element, elementByteSize);
        }
        // 触发监听服务
        listeners.fireCachePut(key, element);
        // 递增统计缓存内存占用
        doAddByteSize(elementByteSize);
        return element.doPut(field, value, elementByteSize);
    }

    @Override
    public boolean hput(String key, String field, String value) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putString(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, int value) {
        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putInt(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, long value) {
        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putLong(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, short value) {
        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putShort(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, double value) {
        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putDouble(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, float value) {
        ProtoBuf buffer = ProtoBuf.wrap(value);
        buffer.putFloat(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, byte[] value) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = ProtoBuf.wrap(value);
        return hput(key, field, buffer, false);
    }

    @Override
    public boolean hput(String key, String field, Object value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putObject(value, schema);
        return hput(key, field, buffer, true);
    }

    @Override
    public boolean hput(String key, String field, Map<?, ?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putMap(value, schema);
        return hput(key, field, buffer, true);
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

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putParam(value, schema);
        return hput(key, field, buffer, true);
    }

    @Override
    public boolean hput(String key, String field, Table<?> value, ProtoSchema schema) {
        if (value == null) {
            return false;
        }

        ProtoBuf buffer = new ProtoBuf(config.isDirectBuffer());
        buffer.putTable(value, schema);
        return hput(key, field, buffer, true);
    }

    @Override
    public boolean hmput(String key, Map<byte[], byte[]> value) {
        if (key == null || value == null) {
            return false;
        }

        Element1 element = cache1.get(key);
        if (element == null) {
            // 设置过期时间
            JvmConfig jvmConfig = config.getJvmConfig();
            int expirationTime = jvmConfig.getExpirationTime();
            if (jvmConfig.isExpirationTimeRandom()) {
                // 设置过期时间随机，避免同一时间有大量缓存过期导致回缓压力大
                int timeMin = jvmConfig.getExpirationTimeRandomMin();
                int timeMax = jvmConfig.getExpirationTimeRandomMax();
                expirationTime = random.nextInt(timeMax - timeMin) + timeMin;
            }
            // 需要作加锁操作避免多线程执行下同一个KEY的数据覆盖
            try {
                lock.writeLock(key.hashCode());
                // 双重检查
                if (!cache1.containsKey(key)) {
                    element = new Element1(key, new ConcurrentHashMap<String, byte[]>());
                    element.setExpirationTime(expirationTime);
                    doAddByteSize(element.getByteSize());
                    cache1.put(key, element);
                }
            } finally {
                lock.writeUnlock(key.hashCode());
            }
        }
        Charset charset = config.getChrset();
        for (Entry<byte[], byte[]> entry : value.entrySet()) {
            String field = new String(entry.getKey(), charset);
            ProtoBuf buffer = ProtoBuf.wrap(entry.getValue());
            // 计算该缓存数据的大概字节大小
            int elementByteSize = doCalcuateElement1ValueSize(key, field, buffer);
            // 递增统计缓存内存占用
            doAddByteSize(elementByteSize);
            element.doPut(field, buffer, elementByteSize);
        }
        // 检查是否超过缓存配置上限，超过则触发回收策略
        if (cacheEvitor != null) {
            cacheEvitor.checkOverLimit(element, 0);
        }
        // 触发监听服务
        listeners.fireCachePut(key, element);
        return true;
    }

    @Override
    public boolean remove(String key) {
        Element element = null;
        if (cache0.containsKey(key)) {
            element = cache0.get(key);
        } else if (cache1.containsKey(key)) {
            element = cache1.get(key);
        }
        // 无论有没有数据都算移除成功
        if (element == null) {
            return true;
        }
        if (!doRemoveInternal(element)) {
            return true;
        }
        // 触发监听服务
        listeners.fireCacheRemove(key, element);
        return true;
    }

    @Override
    public boolean remove(String... keys) {
        for (int i = 0; i < keys.length; i++) {
            remove(keys[i]);
        }
        return true;
    }

    @Override
    public boolean remove(String key, String field) {
        if (key == null || field == null) {
            return false;
        }

        Element1 element = null;
        if (cache1.containsKey(key)) {
            element = cache1.get(key);
        }
        // 无论有没有数据都算移除成功
        if (element == null) {
            return true;
        }
        ProtoBuf buffer = element.remove(field);
        if (buffer == null) {
            return true;
        }
        int byteSize = doCalcuateElement1ValueSize(key, field, buffer);
        doDeductByteSize(byteSize);
        return true;
    }

    @Override
    public boolean remove(String key, String... fields) {
        if (key == null || fields == null) {
            return false;
        }

        for (int i = 0; i < fields.length; i++) {
            remove(key, fields[i]);
        }
        return true;
    }

    @Override
    public synchronized void shutdown() {
    }

    /**
     * 一级/二级缓存数据移除
     */
    private boolean doRemoveInternal(Element removedElement) {
        String key = removedElement.getKey();
        if (removedElement instanceof Element0) {
            removedElement = cache0.remove(key);
        } else if (removedElement instanceof Element1) {
            removedElement = cache1.remove(key);
        }
        if (removedElement == null) {
            return false;
        }
        int byteSize = removedElement.getByteSize();
        doDeductByteSize(byteSize);
        return true;
    }

    /**
     * 缓存过期时的移除
     */
    protected boolean removeExpired(Element element) {
        if (!doRemoveInternal(element)) {
            return false;
        }
        // 触发监听服务
        listeners.fireCacheExpired(element.getKey(), element);
        return true;
    }

    /**
     * 缓存超过限制时的回收移除
     */
    protected boolean removeEvicted(Element element) {
        if (!doRemoveInternal(element)) {
            return false;
        }
        // 触发监听服务
        listeners.fireCacheEvicted(element.getKey(), element);
        return true;
    }

    /**
     * 回收符合条件的缓存数据，
     * 有LFU/LRU等回收策略，如果不设置则默认随机获取某些缓存进行回收处理
     *
     * @param excludeElement 要排除的缓存节点，像当前正在添加的缓存，如果给移除掉那就不能再添加数据了
     * @return 移除成功返回true
     */
    private boolean doRemoveElementChosenByEvictionPolicy(Element excludeElement) {
        Element element = null;
        if (policy != null) {
            List<Element> elements = getRandomElements(excludeElement, DEFAULT_SAMPLE_SIZE);
            element = policy.selectedBasedOnPolicy(elements);
        } else {
            List<Element> elements = getRandomElements(excludeElement, 1);
            int elementSize = elements.size();
            element = elementSize > 0 ? elements.get(elementSize - 1) : null;
        }
        if (element == null) {
            return false;
        }
        return removeEvicted(element);
    }

    /**
     * 随机获取缓存列表中指定数量的缓存数据，
     * 为了提升性能，只获取一二级缓存列表中前30条数据，
     * 主要服务于缓存回收策略服务
     *
     * @param excludeElement 要排除的缓存节点，像当前正在添加的缓存，如果给移除掉那就不能再添加数据了
     * @param size           随机获取的数量
     */
    private List<Element> getRandomElements(Element excludeElement, int size) {
        List<Element> elements = new LinkedList<Element>();
        for (Element element : cache0.values()) {
            if (excludeElement == element) {
                continue;
            }
            if (elements.size() >= size) {
                return elements;
            }
            elements.add(element);
        }
        for (Element element : cache1.values()) {
            if (excludeElement == element) {
                continue;
            }
            if (elements.size() >= size) {
                return elements;
            }
            elements.add(element);
        }
        return elements;
    }

    /**
     * 减少内存容量，
     * 注意JVM缓存中内存不止是ProtoBuf的真正对象字节数据，还包括Key/Element等包装对象的字节大小
     */
    private void doAddByteSize(int size) {
        byteSize.addAndGet(size);
    }

    /**
     * 减少内存容量，
     * 注意JVM缓存中内存不止是ProtoBuf的真正对象字节数据，还包括Key/Element等包装对象的字节大小
     */
    private void doDeductByteSize(int size) {
        byteSize.addAndGet(-size);
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("[Size=").append(size());
        info.append(", Memory=").append(StrUtil.formatByteOutput(byteSize.get()));
        info.append(", Policy=").append(policy.getName());
        info.append("]");
        return info.toString();
    }

    /**
     * 缓存超过上限时的处理服务
     */
    private interface CacheEvictor {
        /**
         * 检查缓存是否超过上限，采用不同的容量上限策略，超过则移除指定的缓存数据
         *
         * @param excludeElement 要排除的缓存节点，像当前正在添加的缓存，如果给移除掉那就不能再添加数据了
         * @param addedSize      新增的字节数
         */
        void checkOverLimit(Element excludeElement, int addedSize);
    }

    /**
     * 缓存条数上限检查服务
     */
    private class MaxElementEvictor implements CacheEvictor {
        @Override
        public void checkOverLimit(Element excludeElement, int addedSize) {
            // 检查是否超过缓存条数上限
            int maxElements = config.getJvmConfig().getMaxElements();
            int elementSize = size();
            if (maxElements > 0 && elementSize >= maxElements) {
                // 已经超过上限，触发回收策略
                int evict = Math.min(elementSize - maxElements + 1, MAX_EVICTION_RATIO);
                for (int i = 0; i < evict; i++) {
                    if (!doRemoveElementChosenByEvictionPolicy(excludeElement)) {
                        // 已经没有适合的缓存节点可以移除了
                        break;
                    }
                }
            }
        }
    }

    /**
     * 缓存内存容量上限检查服务
     */
    private class MaxMemoryEvictor implements CacheEvictor {
        @Override
        public void checkOverLimit(Element excludeElement, int addedSize) {
            // 检查是否超过缓存内存容量上限
            long maxMemory = config.getJvmConfig().getMaxMemory();
            if (maxMemory > 0 && (byteSize.get() + addedSize) > maxMemory) {
                long missingSize = (byteSize.get() + addedSize) - maxMemory;
                // 已经超过上限，触发回收策略
                int evict = 0;
                while (missingSize > 0) {
                    if (evict++ > MAX_EVICTION_RATIO) {
                        break;
                    }
                    if (!doRemoveElementChosenByEvictionPolicy(excludeElement)) {
                        // 已经没有适合的缓存节点可以移除了
                        break;
                    }
                    missingSize = (byteSize.get() + addedSize) - maxMemory;
                }
            }
        }
    }

    private int doCalcuateElement1ValueSize(String key, String field, ProtoBuf value) {
        Charset charset = config.getChrset();
        return key.getBytes(charset).length +
                field.getBytes(charset).length + value.readableBytes() + Element1.PROTOBUF_SIZE;
    }
}
