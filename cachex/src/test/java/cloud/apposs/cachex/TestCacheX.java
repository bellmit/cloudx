package cloud.apposs.cachex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.Where;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.util.Param;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

public class TestCacheX {
    public static final String SQLITE_URL = "jdbc:sqlite://C:/user.tdb";

    //	public static final String HOST = "172.17.1.225";
    public static final String HOST = "192.168.1.5";
    public static final int PORT = 6379;

    // 表字段
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CLASS = "class";
    // 表元信息
    public static final ProtoSchema USER_SCHEMA = ProtoSchema.mapSchema();

    static {
        USER_SCHEMA.addKey(FIELD_ID, Integer.class);
        USER_SCHEMA.addKey(FIELD_NAME, String.class);
        USER_SCHEMA.addKey(FIELD_CLASS, Integer.class);
    }

    private ParamCacheX<MyCacheKey> cachex;

    @Before
    public void before() throws Exception {
        CacheXConfig config = new CacheXConfig();
        config.setDevelop(true);
        config.getDbConfig().setJdbcUrl(SQLITE_URL);
        config.setDialect(SqlBuilder.DIALECT_SQLITE);

//		config.setCache(Cache.CACHE_REDIS);
//		RedisConfig redisConfig = config.getRedisConfig();
//		redisConfig.addServer(new RedisServer(HOST, PORT));
//		redisConfig.setCacheType(RedisConfig.REDIS_CACHE_SINGLE);

        config.setWriteBehind(false);
        ParamCacheLoader loader = new ParamCacheLoader();
        cachex = new ParamCacheX<MyCacheKey>(config, loader);

        // 先创建表
        Metadata metadata = new Metadata("user", "UTF8");
        metadata.addPrimaryColumn(FIELD_ID, Metadata.COLUMN_TYPE_INT, 11);
        metadata.addColumn(FIELD_NAME, Metadata.COLUMN_TYPE_STRING, 50, true, null);
        metadata.addColumn(FIELD_CLASS, Metadata.COLUMN_TYPE_INT, 11);
        if (!cachex.getDao().exist(metadata)) {
            cachex.getDao().create(metadata, false);
        }
    }

    @After
    public void after() throws Exception {
        cachex.shutdown();
    }

    @Test
    public void testPut() throws Exception {
        Param data = new Param();
        data.setString(FIELD_NAME, "John");
        data.setInt(FIELD_CLASS, 101);
        // 因为AID是靠数据库递增，所以 一开始并不知道数值
        MyCacheKey key = new MyCacheKey();
        cachex.put(key, data, USER_SCHEMA);
        System.out.println(cachex.get(key, USER_SCHEMA));
    }

    @Test
    public void testPutBatch() throws Exception {
        List<Param> values = new ArrayList<Param>();
        Param data1 = new Param();
        data1.setString(FIELD_NAME, "Donald");
        data1.setInt(FIELD_CLASS, 101);
        values.add(data1);
        Param data2 = new Param();
        data2.setString(FIELD_NAME, "Lily");
        data2.setInt(FIELD_CLASS, 101);
        values.add(data2);
        // 因为AID是靠数据库递增，所以 一开始并不知道数值
        List<MyCacheKey> keys = new ArrayList<MyCacheKey>();
        MyCacheKey key = new MyCacheKey();
        int count = cachex.put(key, values, USER_SCHEMA);
        System.out.println(count);
    }

    @Test
    public void testGet() throws Exception {
        MyCacheKey key = new MyCacheKey(1);
        cachex.get(key, USER_SCHEMA);
        System.out.println(cachex.get(key, USER_SCHEMA));
    }

    @Test
    public void testSelect() throws Exception {
        MyCacheKey.QueryKey key = new MyCacheKey.QueryKey("select");
        Query query = Query.builder();
        query.where(FIELD_CLASS, Where.EQ, 101);
        Param data = cachex.select(key, USER_SCHEMA, query);
        Param data2 = cachex.select(key, USER_SCHEMA, query);
        System.out.println(data);
        System.out.println(data2);
    }

    @Test
    public void testQuery() throws Exception {
        MyCacheKey.QueryKey key = new MyCacheKey.QueryKey("search1");
        Query query = Query.builder();
        query.where(FIELD_CLASS, Where.EQ, 101);
        List<Param> dataList = cachex.query(key, USER_SCHEMA, query, true);
        List<Param> dataList2 = cachex.query(key, USER_SCHEMA, query, true);
        System.out.println(dataList);
        System.out.println(dataList2);
    }

    @Test
    public void testGetBatch() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            MyCacheKey key = new MyCacheKey(1);
            cachex.get(key, USER_SCHEMA);
        }
        System.out.println("batch execute:" + (System.currentTimeMillis() - start));
    }

    @Test
    public void testDelete() throws Exception {
        MyCacheKey key = new MyCacheKey(1);
        cachex.delete(key);
        System.out.println(cachex.get(key, USER_SCHEMA));
    }

    @Test
    public void testUpdate() throws Exception {
        MyCacheKey key = new MyCacheKey(2);
        Param value = new Param();
        value.setString("name", "WayKen100");
        cachex.update(key, value, USER_SCHEMA);
        System.out.println(cachex.get(key, USER_SCHEMA));
    }

    @Test
    public void testGetArgs() throws Exception {
        MyCacheKey key = new MyCacheKey(1);
        int productId = 10081;
        cachex.get(key, USER_SCHEMA, productId);
        System.out.println(cachex.get(key, USER_SCHEMA));
    }

    @Test
    public void testAsyncGet() throws Exception {
        long start = System.currentTimeMillis();
        MyCacheKey key = new MyCacheKey(1);
        final CountDownLatch cdl = new CountDownLatch(1);
        cachex.asyncGet(key, USER_SCHEMA, new AsyncCacheXHandler<Param>() {
            @Override
            public void operationComplete(Param result, Throwable cause) {
                System.out.println("Async Get Resut:" + result);
                cdl.countDown();
            }
        });
        System.out.println("execute:" + (System.currentTimeMillis() - start));
        cdl.await();
    }

    static class MyCacheKey extends AbstractCacheKey<Integer> {
        private static final String CACHE_KEY_PREFIX = "site-";

        public MyCacheKey() {
            super(CACHE_KEY_PREFIX);
        }

        public MyCacheKey(int aid) {
            super(aid, CACHE_KEY_PREFIX);
        }

        static class QueryKey extends AbstractCacheKey<String> {
            private final String key;

            public QueryKey(String key) {
                this.key = key;
            }

            @Override
            public String getCacheKey() {
                return key;
            }
        }
    }

    static class ParamCacheLoader extends CacheLoaderAdapter<MyCacheKey, Param> {
        private static final String TABLE = "user";

        @Override
        public int add(MyCacheKey key, Param value, ProtoSchema schema,
                       CacheX<MyCacheKey, Param> cache, Ref<Object> idRef, Object... args) throws Exception {
            Dao dao = cache.getDao();
            int count = dao.insert(TABLE, new Entity(value), idRef);
            // 添加成功，设置主键
            if (count > 0 && idRef.value() != null) {
                value.setInt(FIELD_ID, (Integer) idRef.value());
            }
            return count;
        }

        @Override
        public int add(List<Param> values, ProtoSchema schema, CacheX<MyCacheKey, Param> cache, List<Object> idRefs, Object... args) throws Exception {
            Dao dao = cache.getDao();
            List<Entity> datas = new ArrayList<Entity>();
            for (Param value : values) {
                datas.add(new Entity(value));
            }
            int count = dao.insert(TABLE, datas, schema, idRefs);
            return count;
        }

        @Override
        public Param load(MyCacheKey key,
                          ProtoSchema schema, CacheX<MyCacheKey, Param> cache, Object... args) throws Exception {
            if (args != null && args.length > 0) {
                System.out.println("loader recv args:" + args[0]);
            }
            // 查询数据加载到缓存中
            int aid = key.getPrimary();
            Dao dao = cache.getDao();
            Entity info = dao.select(TABLE, FIELD_ID, aid);
            if (info == null) {
                return null;
            }
            return info.getDatas();
        }

        @Override
        public Param select(CacheKey<?> key, ProtoSchema schema,
                            Query query, CacheX<MyCacheKey, Param> cache, Object... args) throws Exception {
            Dao dao = cache.getDao();
            return dao.select(TABLE, query);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<Param> query(CacheKey<?> key, ProtoSchema schema,
                                 Query query, AbstractCacheX<MyCacheKey, Param> cache, Object... args) throws Exception {
            Dao dao = cache.getDao();
            List datas = dao.query(TABLE, query);
            return datas;
        }

        @Override
        public int delete(MyCacheKey key, CacheX<MyCacheKey, Param> cache, Object... args) throws Exception {
            // 简单删除主键
            Dao dao = cache.getDao();
            return dao.delete(TABLE, FIELD_ID, key.getPrimary());
        }

        @Override
        public int update(MyCacheKey key, Param value, ProtoSchema schema,
                          CacheX<MyCacheKey, Param> cache, Object... args) throws Exception {
            // 简单更新实体数据
            Dao dao = cache.getDao();
            Entity entity = new Entity(FIELD_ID);
            entity.setIdentity(key.getPrimary());
            entity.putAll(value);
            return dao.update(TABLE, entity);
        }
    }
}
