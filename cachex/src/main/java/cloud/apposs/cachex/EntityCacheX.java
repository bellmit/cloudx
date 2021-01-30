package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Param;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于数据类型为{@link Entity}的数据服务
 */
public class EntityCacheX<K extends CacheKey> extends AbstractCacheX<K, Entity> {
    /**
     * 数据库查询不存在时的缓存，用于将不存在(NOT FOUND)的数据进行缓存避免数据一直查询
     */
    public static final Param DATA_NOT_FOUND = Entity.builder();

    public EntityCacheX(CacheXConfig config, CacheLoader<K, Entity> loader) {
        super(config, loader);
    }

    public EntityCacheX(CacheXConfig config, CacheLoader<K, Entity> loader, int lockLength) {
        super(config, loader, lockLength);
    }

    /**
     * 对查询的Schema进行再封装，减少业务关心太多细节
     * {@inheritDoc}
     */
    @Override
    public List<Entity> query(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception {
        return super.query(key, ProtoSchema.listSchema(Param.class, schema), query, args);
    }

    /**
     * 对查询的Schema进行再封装，减少业务关心太多细节
     * {@inheritDoc}
     */
    @Override
    public void asyncQuery(CacheKey<?> key, ProtoSchema schema, Query query,
                           AsyncCacheXHandler<List<Entity>> handler, Object... args) throws Exception {
        super.asyncQuery(key, ProtoSchema.listSchema(Param.class, schema), query, handler, args);
    }

    @Override
    public boolean doPut(String key, Entity value, ProtoSchema schema, Object... args) {
        Param fieldData = value.getDatas();
        return cache.put(key, fieldData, schema);
    }

    @Override
    protected boolean doPutList(String key, List<Entity> values, ProtoSchema schema, Object... args) {
        List<Param> fileDatas = new ArrayList<Param>(values.size());
        for (Entity value : values) {
            fileDatas.add(value.getDatas());
        }
        return cache.put(key, fileDatas, schema);
    }

    @Override
    public Entity doGet(String key, ProtoSchema schema, Object... args) {
        Param data = cache.getParam(key, schema);
        return new Entity(data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Entity> doGetList(String key, ProtoSchema schema, Object... args) {
        List<Param> values = (List<Param>) cache.getList(key, schema);
        List<Entity> entities = new ArrayList<Entity>(values.size());
        for (Param value : values) {
            entities.add(new Entity(value));
        }
        return entities;
    }

    @Override
    public boolean checkExist(Entity value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public boolean doHputAll(String key, List<Entity> value, ProtoSchema schema, Object... args) {
        Charset charset = config.getChrset();
        Map<byte[], byte[]> infoList = new HashMap<byte[], byte[]>();
        for (Entity entity : value) {
            String mapKey = loader.getField(entity);
            if (mapKey == null) {
                throw new IllegalStateException("CacheLoader get field null error");
            }
            Param data = entity.getDatas();
            ProtoBuf buffer = ProtoBuf.wrap(data, schema);
            infoList.put(mapKey.getBytes(charset), buffer.array());
        }
        return cache.hmput(key, infoList);
    }

    @Override
    public boolean doHput(String key, Object field, Entity value, ProtoSchema schema, Object... args) {
        Param data = value.getDatas();
        return cache.hput(key, field.toString(), data, schema);
    }

    @Override
    public boolean checkHExist(Entity value) {
        return value != null && !value.isEmpty();
    }

    @Override
    public Entity doHget(String key, Object field, ProtoSchema schema, Object... args) {
        Param data = cache.hgetParam(key, field.toString(), schema);
        return new Entity(data);
    }

    @Override
    public List<Entity> doHgetAll(String key, ProtoSchema schema, Object... args) {
        List<Param> dataList = cache.hgetAllParam(key, schema);
        if (dataList == null) {
            return null;
        }
        List<Entity> entityList = new LinkedList<Entity>();
        for (Param data : dataList) {
            entityList.add(new Entity(data));
        }
        return entityList;
    }
}
