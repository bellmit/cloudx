package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Param;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于数据类型为{@link Param}的数据服务
 */
public class ParamCacheX<K extends CacheKey> extends AbstractCacheX<K, Param> {
    /**
     * 数据库查询不存在时的缓存，用于将不存在(NOT FOUND)的数据进行缓存避免数据一直查询
     */
    public static final Param DATA_NOT_FOUND = Param.builder();

    public ParamCacheX(CacheXConfig config, CacheLoader<K, Param> loader) {
        super(config, loader);
    }

    public ParamCacheX(CacheXConfig config, CacheLoader<K, Param> loader, int lockLength) {
        super(config, loader, lockLength);
    }

    /**
     * 对查询的Schema进行再封装，减少业务关心太多细节
     * {@inheritDoc}
     */
    @Override
    public List<Param> query(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception {
        return super.query(key, ProtoSchema.listSchema(Param.class, schema), query, args);
    }

    /**
     * 对查询的Schema进行再封装，减少业务关心太多细节
     * {@inheritDoc}
     */
    @Override
    public void asyncQuery(CacheKey<?> key, ProtoSchema schema,
                           Query query, AsyncCacheXHandler<List<Param>> handler, Object... args) throws Exception {
        super.asyncQuery(key, ProtoSchema.listSchema(Param.class, schema), query, handler, args);
    }

    @Override
    public boolean doPut(String key, Param value, ProtoSchema schema, Object... args) {
        return cache.put(key, value, schema);
    }

    @Override
    protected boolean doPutList(String key, List<Param> values, ProtoSchema schema, Object... args) {
        return cache.put(key, values, schema);
    }

    @Override
    public Param doGet(String key, ProtoSchema schema, Object... args) {
        return cache.getParam(key, schema);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Param> doGetList(String key, ProtoSchema schema, Object... args) {
        return (List<Param>) cache.getList(key, schema);
    }

    @Override
    public boolean checkExist(Param value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public boolean doHputAll(String key, List<Param> value, ProtoSchema schema, Object... args) {
        Charset charset = config.getChrset();
        Map<byte[], byte[]> infoList = new HashMap<byte[], byte[]>();
        for (Param info : value) {
            String mapKey = loader.getField(info);
            if (mapKey == null) {
                throw new IllegalStateException("CacheLoader get field null error");
            }
            ProtoBuf buffer = ProtoBuf.wrap(info, schema);
            infoList.put(mapKey.getBytes(charset), buffer.array());
        }
        return cache.hmput(key, infoList);
    }

    @Override
    public boolean doHput(String key, Object field, Param value, ProtoSchema schema, Object... args) {
        return cache.hput(key, field.toString(), value, schema);
    }

    @Override
    public Param doHget(String key, Object field, ProtoSchema schema, Object... args) {
        return cache.hgetParam(key, field.toString(), schema);
    }

    @Override
    public boolean checkHExist(Param value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public List<Param> doHgetAll(String key, ProtoSchema schema, Object... args) {
        return cache.hgetAllParam(key, schema);
    }
}
