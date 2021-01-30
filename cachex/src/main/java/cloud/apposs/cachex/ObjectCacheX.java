package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对象数据存取
 */
public class ObjectCacheX<K extends CacheKey> extends AbstractCacheX<K, Object> {
    /**
     * 数据库查询不存在时的缓存，用于将不存在(NOT FOUND)的数据进行缓存避免数据一直查询
     */
    public static final Object DATA_NOT_FOUND = new Object();

    public ObjectCacheX(CacheXConfig config, CacheLoader<K, Object> loader) {
        super(config, loader);
    }

    public ObjectCacheX(CacheXConfig config, CacheLoader<K, Object> loader, int lockLength) {
        super(config, loader, lockLength);
    }

    @Override
    public boolean doPut(String key, Object value, ProtoSchema schema, Object... args) {
        return cache.put(key, value, schema);
    }

    @Override
    protected boolean doPutList(String key, List<Object> values, ProtoSchema schema, Object... args) {
        return cache.put(key, values, schema);
    }

    @Override
    public Object doGet(String key, ProtoSchema schema, Object... args) {
        Class<?> clazz = schema.getClass();
        return cache.getObject(key, clazz, schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> doGetList(String key, ProtoSchema schema, Object... args) {
        return (List<Object>) cache.getList(key, schema);
    }

    @Override
    public boolean checkExist(Object value) {
        return value != null && value != DATA_NOT_FOUND;
    }

    @Override
    public boolean doHputAll(String key, List<Object> value, ProtoSchema schema, Object... args) {
        Charset charset = config.getChrset();
        Map<byte[], byte[]> infoList = new HashMap<byte[], byte[]>();
        for (Object info : value) {
            String mapKey = loader.getField(info);
            if (mapKey == null) {
                throw new IllegalStateException("CacheLoader get field null error");
            }
            ProtoBuf buffer = ProtoBuf.allocate();
            buffer.putObject(info, schema);
            infoList.put(mapKey.getBytes(charset), buffer.array());
        }
        return cache.hmput(key, infoList);
    }

    @Override
    public boolean doHput(String key, Object field, Object value, ProtoSchema schema, Object... args) {
        return cache.hput(key, field.toString(), value, schema);
    }

    @Override
    public boolean checkHExist(Object value) {
        return value != null;
    }

    @Override
    public Object doHget(String key, Object field, ProtoSchema schema, Object... args) {
        Class<?> clazz = schema.getClass();
        return cache.hgetObject(key, field.toString(), clazz, schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> doHgetAll(String key, ProtoSchema schema, Object... args) {
        Class<?> clazz = schema.getClass();
        return (List<Object>) cache.hgetAllObject(key, clazz, schema);
    }
}
