package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Table;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于数据类型为{@link Table}的数据服务
 */
public class TableCacheX<K extends CacheKey> extends AbstractCacheX<K, Table<?>> {
    /**
     * 数据库查询不存在时的缓存，用于将不存在(NOT FOUND)的数据进行缓存避免数据一直查询
     */
    public static final Table DATA_NOT_FOUND = Table.builder();

    public TableCacheX(CacheXConfig config, CacheLoader<K, Table<?>> loader) {
        super(config, loader);
    }

    public TableCacheX(CacheXConfig config, CacheLoader<K, Table<?>> loader, int lockLength) {
        super(config, loader, lockLength);
    }

    @Override
    public boolean doPut(String key, Table<?> value, ProtoSchema schema, Object... args) {
        return cache.put(key, value, schema);
    }

    @Override
    protected boolean doPutList(String key, List<Table<?>> values, ProtoSchema schema, Object... args) {
        return cache.put(key, values, schema);
    }

    @Override
    public Table<?> doGet(String key, ProtoSchema schema, Object... args) {
        return cache.getTable(key, schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Table<?>> doGetList(String key, ProtoSchema schema, Object... args) {
        return (List<Table<?>>) cache.getList(key, schema);
    }

    @Override
    public boolean checkExist(Table<?> value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public boolean doHputAll(String key, List<Table<?>> value, ProtoSchema schema, Object... args) {
        Charset charset = config.getChrset();
        Map<byte[], byte[]> infoList = new HashMap<byte[], byte[]>();
        for (Table<?> info : value) {
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
    public boolean doHput(String key, Object field, Table<?> value, ProtoSchema schema, Object... args) {
        return cache.hput(key, field.toString(), value, schema);
    }

    @Override
    public Table<?> doHget(String key, Object field, ProtoSchema schema, Object... args) {
        return cache.hgetTable(key, field.toString(), schema);
    }

    @Override
    public boolean checkHExist(Table<?> value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public List<Table<?>> doHgetAll(String key, ProtoSchema schema, Object... args) {
        return cache.hgetAllTable(key, schema);
    }
}
