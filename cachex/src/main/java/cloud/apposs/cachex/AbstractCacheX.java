package cloud.apposs.cachex;

import cloud.apposs.cachex.memory.CacheManager;
import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.threadx.Future;
import cloud.apposs.threadx.FutureListener;
import cloud.apposs.threadx.ThreadPool;
import cloud.apposs.threadx.ThreadPoolFactory;
import cloud.apposs.util.Ref;
import cloud.apposs.util.StrUtil;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 数据抽象，
 * 注意因为对数据和缓存多了一层封装，所以更多只针对主键的缓存，其他复杂查询则需要业务自己通过dao和cache来实现缓存
 */
public abstract class AbstractCacheX<K extends CacheKey, V> implements CacheX<K, V> {
    /**
     * 数据操作最终一致前的过期时间，一分钟
     */
    public static final int DEAFULT_CACHE_EXPIRE_TIME = 60 * 1000;

    /**
     * 数据配置
     */
    protected final CacheXConfig config;

    /**
     * 数据库服务
     */
    protected final Dao dao;

    /**
     * 缓存服务
     */
    protected final CacheManager cache;

    /**
     * 缓存数据加载服务
     */
    protected final CacheLoader<K, V> loader;

    /**
     * 缓存锁，用于从数据库中加载数据时加锁来保证服务原子性
     */
    protected final CacheLock lock;

    /**
     * 线程池，主要用于异步写缓存数据
     */
    private final ThreadPool threadPool;

    /**
     * 缓存统计服务
     */
    protected final CacheXStatistics statistics = new CacheXStatistics();

    public AbstractCacheX(CacheXConfig config, CacheLoader<K, V> loader) {
        this(config, loader, CacheLock.DEFAULT_LOCK_LENGTH);
    }

    public AbstractCacheX(CacheXConfig config, CacheLoader<K, V> loader, int lockLength) {
        if (lockLength <= 0) {
            throw new IllegalArgumentException("lockLength");
        }
        this.config = config;
        this.dao = new Dao(config);
        this.cache = new CacheManager(config);
        this.loader = loader;
        this.lock = new CacheLock(lockLength);
		if (config.isAsync()) {
            this.threadPool = ThreadPoolFactory.createCachedThreadPool();
        } else {
            this.threadPool = null;
        }
        this.loader.initialize(this);
    }

    public CacheXConfig getConfig() {
        return config;
    }

    @Override
    public Dao getDao() {
        return dao;
    }

    @Override
    public Dao getDao(String source) {
        return null;
    }

    @Override
    public CacheManager getCache() {
        return cache;
    }

    @Override
    public CacheXStatistics getStatistics() {
        return statistics;
    }

    @Override
    public int put(K key, V value, ProtoSchema schema, Object... args) throws Exception {
        if (key == null || value == null) {
            return -1;
        }

        return doPutCacheX(key, value, schema, config.isWriteBehind(), args);
    }

    @Override
    public int put(K key, List<V> values, ProtoSchema schema, Object... args) throws Exception {
        if (key == null || values == null) {
            return -1;
        }
        return doPutCacheX(key, values, schema, config.isWriteBehind(), args);
    }

    @Override
    public V get(K key, ProtoSchema schema, Object... args) throws Exception {
        if (key == null) {
            return null;
        }

        return doGetCacheX(key, schema, config.isWriteBehind(), args);
    }

    @Override
    public V select(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception {
        return doSelectCacheX(key, schema, query, config.isWriteBehind(), args);
    }

    @Override
    public List<V> query(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception {
        return doQueryCacheX(key, schema, query, config.isWriteBehind(), args);
    }

    @Override
    public boolean exist(K key, ProtoSchema schema, Object... args) throws Exception {
        V value = get(key, schema, args);
        return checkExist(value);
    }

    @Override
    public boolean exist(K key, ProtoSchema schema, Query query, Object... args) throws Exception {
        List<V> values = query(key, schema, query, args);
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (V value : values) {
            if (checkExist(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int delete(K key, final Object... args) throws Exception {
        if (key == null) {
            return -1;
        }

        return doDeleteCacheX(key, args);
    }

    @Override
    public int update(K key, V value, ProtoSchema schema, final Object... args) throws Exception {
        if (key == null || value == null) {
            return -1;
        }

        return doUpdateCacheX(key, value, schema, args);
    }

    @Override
    public int hput(K key, Object field, V value, ProtoSchema schema, Object... args) throws Exception {
        if (key == null || field == null) {
            return -1;
        }

        return doHputCacheX(key, field, value, schema, config.isWriteBehind(), args);
    }

    @Override
    public V hget(K key, Object field, ProtoSchema schema, Object... args) throws Exception {
        if (key == null || field == null) {
            return null;
        }

        return doHgetCacheX(key, field, schema, config.isWriteBehind(), args);
    }

    @Override
    public boolean hexist(K key, Object field, ProtoSchema schema, Object... args) throws Exception {
        V value = hget(key, field, schema, args);
        return checkHExist(value);
    }

    @Override
    public List<V> hgetAll(K key, ProtoSchema schema, Object... args) throws Exception {
        if (key == null) {
            return null;
        }

        return doHgetAllCacheX(key, schema, args);
    }

    @Override
    public int hdelete(K key, Object field, final Object... args) throws Exception {
        if (key == null || field == null) {
            return -1;
        }

        return doHdeleteCacheX(key, field, args);
    }

    @Override
    public int hdelete(K key, Object[] fields, final Object... args) throws Exception {
        if (key == null || fields == null) {
            return -1;
        }

        return doHdeleteCacheX(key, fields, args);
    }

    @Override
    public int hupdate(K key, Object field, V value,
                       ProtoSchema schema, final Object... args) throws Exception {
        if (key == null || field == null) {
            return -1;
        }

        return doHupdateCacheX(key, field, value, schema, args);
    }

    @Override
    public void asyncPut(K key, V value, ProtoSchema schema, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || value == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doPutCacheX(key, value, schema, false, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncGet(K key, ProtoSchema schema, AsyncCacheXHandler<V> handler, Object... args) {
        if (key == null || handler == null || threadPool == null) {
            return;
        }

        Future<V> future = threadPool.submit(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return doGetCacheX(key, schema, false, args);
            }
        });
        future.addListener(new FutureListener<Future<V>>() {
            @Override
            public void executeComplete(Future<V> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncSelect(K key, ProtoSchema schema, Query query, AsyncCacheXHandler<V> handler, Object... args) {
        if (handler == null || threadPool == null) {
            return;
        }

        Future<V> future = threadPool.submit(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return doSelectCacheX(key, schema, query, false, args);
            }
        });
        future.addListener(new FutureListener<Future<V>>() {
            @Override
            public void executeComplete(Future<V> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncQuery(CacheKey<?> key, ProtoSchema schema,
                           Query query, AsyncCacheXHandler<List<V>> handler, Object... args) throws Exception {
        if (handler == null || threadPool == null) {
            return;
        }

        Future<List<V>> future = threadPool.submit(new Callable<List<V>>() {
            @Override
            public List<V> call() throws Exception {
                return doQueryCacheX(key, schema, query, false, args);
            }
        });
        future.addListener(new FutureListener<Future<List<V>>>() {
            @Override
            public void executeComplete(Future<List<V>> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asynDelete(K key, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doDeleteCacheX(key, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncUpdate(K key, V value, ProtoSchema schema, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || value == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doUpdateCacheX(key, value, schema, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHput(K key, Object field, V value,
                          ProtoSchema schema, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || field == null || value == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doHputCacheX(key, field, value, schema, false, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHget(K key, Object field, ProtoSchema schema, AsyncCacheXHandler<V> handler, Object... args) {
        if (key == null || field == null || handler == null || threadPool == null) {
            return;
        }

        Future<V> future = threadPool.submit(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return doHgetCacheX(key, field, schema, false, args);
            }
        });
        future.addListener(new FutureListener<Future<V>>() {
            @Override
            public void executeComplete(Future<V> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHgetAll(K key, ProtoSchema schema, AsyncCacheXHandler<List<V>> handler, Object... args) {
        if (key == null || handler == null || threadPool == null) {
            return;
        }

        Future<List<V>> future = threadPool.submit(new Callable<List<V>>() {
            @Override
            public List<V> call() throws Exception {
                return doHgetAllCacheX(key, schema, args);
            }
        });
        future.addListener(new FutureListener<Future<List<V>>>() {
            @Override
            public void executeComplete(Future<List<V>> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHdelete(K key, Object field, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || field == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doHdeleteCacheX(key, field, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHdelete(K key, Object[] fields, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || fields == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doHdeleteCacheX(key, fields, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    @Override
    public void asyncHupdate(K key, Object field, V value,
                             ProtoSchema schema, AsyncCacheXHandler<Integer> handler, Object... args) {
        if (key == null || field == null || value == null || handler == null || threadPool == null) {
            return;
        }

        Future<Integer> future = threadPool.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return doHupdateCacheX(key, field, value, schema, args);
            }
        });
        future.addListener(new FutureListener<Future<Integer>>() {
            @Override
            public void executeComplete(Future<Integer> future, Throwable cause) {
                handler.operationComplete(future.getNow(), cause);
            }
        });
    }

    /**
     * 关闭数据库连接池、缓存连接、线程池，释放资源
     */
    @Override
    public synchronized void shutdown() {
        dao.shutdown();
        cache.shutdown();
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private int doPutCacheX(K key, V value, ProtoSchema schema, boolean writeBehind, Object... args) throws Exception {
        // 先将数据存储到数据库中
        Ref<Object> idRef = new Ref<Object>();
        int count = 0;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.add(key, value, schema, this, idRef, args);
                if (count > 0 && idRef.value() != null) {
                    key.setPrimary(idRef.value());
                }
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个相同请求会有数据覆盖风险
            count = loader.add(key, value, schema, this, idRef, args);
            if (count > 0 && idRef.value() != null) {
                key.setPrimary(idRef.value());
            }
        }
        // 数据存储成功后再将数据异步写入到缓存中
        if (count > 0) {
            doWriteCache(key, value, schema, writeBehind, args);
        }
        return count;
    }

    private int doPutCacheX(K key, List<V> values, ProtoSchema schema,
                            boolean writeBehind, Object... args) throws Exception {
        // 先将数据存储到数据库中
        int count = 0;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.add(values, schema, this, null, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个相同请求会有数据覆盖风险
            count = loader.add(values, schema, this, null, args);
        }

        return count;
    }

    private V doGetCacheX(K key, final ProtoSchema schema, boolean writeBehind, Object... args) throws Exception {
        // 先从缓存中获取数据
        V value = doGet(key.getCacheKey(), schema);
        if (value != null) {
            // 数据有缓存，但其实缓存定义的空数据，
            // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
            // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
            if (!checkExist(value)) {
                return null;
            }
            statistics.addHitCount();
            return value;
        }
        // 缓存数据不存在，从Dao回源加载数据
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                // 双重检测，继续判断缓存中是否存在
                value = doGet(key.getCacheKey(), schema);
                if (value != null) {
                    return value;
                }
                value = loader.load(key, schema, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次读取数据库，加重数据库负担
            value = loader.load(key, schema, this, args);
        }
        // 将数据库中查询的数据添加到缓存中以便于下次直接从缓存获取
        if (value != null) {
            doWriteCache(key, value, schema, writeBehind, args);
        }
        statistics.addMissCount();
        // 数据有缓存，但其实缓存的是定义的空数据，
        // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
        // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
        if (!checkExist(value)) {
            return null;
        }
        return value;
    }

    private V doSelectCacheX(CacheKey<?> key, ProtoSchema schema,
                             Query query, boolean writeBehind, Object... args) throws Exception {
        // 先从缓存中获取数据
        V value = null;
        if (!StrUtil.isEmpty(key.getCacheKey())) {
            value = doGet(key.getCacheKey(), schema);
            if (value != null) {
                // 数据有缓存，但其实缓存定义的空数据，
                // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
                // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
                if (!checkExist(value)) {
                    return null;
                }
                statistics.addHitCount();
                return value;
            }
        }
        // 缓存数据不存在，从Dao回源加载数据
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                // 双重检测，继续判断缓存中是否存在
                if (!StrUtil.isEmpty(key.getCacheKey())) {
                    value = doGet(key.getCacheKey(), schema);
                    if (value != null) {
                        return value;
                    }
                }
                value = loader.select(key, schema, query, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次读取数据库，加重数据库负担
            value = loader.select(key, schema, query, this, args);
        }
        // 将数据库中查询的数据添加到缓存中以便于下次直接从缓存获取
        if (value != null) {
            if (!StrUtil.isEmpty(key.getCacheKey())) {
                doWriteCache(key, value, schema, writeBehind, args);
            }
        }
        statistics.addMissCount();
        // 数据有缓存，但其实缓存的是定义的空数据，
        // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
        // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
        if (!checkExist(value)) {
            return null;
        }
        return value;
    }

    private List<V> doQueryCacheX(CacheKey<?> key, ProtoSchema schema,
                Query query, boolean writeBehind, Object[] args) throws Exception {
        // 先从缓存中获取数据
        List<V> values = null;
        if (!StrUtil.isEmpty(key.getCacheKey())) {
            values = doGetList(key.getCacheKey(), schema);
            if (values != null) {
                statistics.addHitCount();
                return values;
            }
        }
        // 缓存数据不存在，从Dao回源加载数据
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                // 双重检测，继续判断缓存中是否存在
                if (!StrUtil.isEmpty(key.getCacheKey())) {
                    values = doGetList(key.getCacheKey(), schema);
                    if (values != null) {
                        return values;
                    }
                }
                values = loader.query(key, schema, query, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次读取数据库，加重数据库负担
            values = loader.query(key, schema, query, this, args);
        }
        // 将数据库中查询的数据添加到缓存中以便于下次直接从缓存获取
        if (values != null) {
            if (!StrUtil.isEmpty(key.getCacheKey())) {
                doWriteCacheList(key, values, schema, writeBehind, args);
            }
        }
        statistics.addMissCount();
        return values;
    }

    private int doDeleteCacheX(K key, Object... args) throws Exception {
        // 先从缓存中移除数据
        boolean success = cache.remove(key.getCacheKey());
        if (!success) {
            // 缓存删除失败直接退出，
            // 不允许缓存删除不成功数据库却删除成功，会有脏数据
            return -1;
        }

        // 再从数据库中删除数据
        int count = -1;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.delete(key, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次删除数据库
            count = loader.delete(key, this, args);
        }
        // 再删除缓存，
        // 因为有可能要高并发情况下该方法在一开始删除了缓存，但同时又有另外的请求又重新加载了缓存
        if (count != -1) {
            cache.remove(key.getCacheKey());
        }
        return count;
    }

    private int doUpdateCacheX(K key, V value, ProtoSchema schema, Object... args) throws Exception {
        // 先从缓存中移除数据，
        // 不更新缓存数据，操作比较复杂，下次获取数据如果没有直接回源即可
        boolean success = cache.remove(key.getCacheKey());
        if (!success) {
            // 缓存删除失败直接退出，
            // 不允许缓存删除不成功数据库却更新成功，会有脏数据
            return -1;
        }
        // 再从数据库中更新数据
        int count = -1;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.update(key, value, schema, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次更新数据库
            count = loader.update(key, value, schema, this, args);
        }
        // 再删除缓存，
        // 因为有可能要高并发情况下该方法在一开始删除了缓存，但同时又有另外的请求又重新加载了缓存
        if (count != -1) {
            cache.remove(key.getCacheKey());
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private int doHputCacheX(K key, Object field, V value,
                             ProtoSchema schema, boolean writeBehind, Object... args) throws Exception {
        // 先将数据存储到数据库中
        int index = key.getLockIndex();
        Ref<Object> idRef = new Ref<Object>();
        int count = 0;
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.hadd(key, field, value, schema, this, idRef, args);
                if (count > 0 && idRef.value() != null) {
                    key.setPrimary(idRef.value());
                }
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个相同请求会有数据覆盖风险
            count = loader.hadd(key, field, value, schema, this, idRef, args);
            if (count > 0 && idRef.value() != null) {
                key.setPrimary(idRef.value());
            }
        }
        // 数据存储成功后再将数据异步写入到缓存中
        if (count > 0) {
            doWriteCacheH(key, field, value, schema, writeBehind, args);
        }
        return count;
    }

    private V doHgetCacheX(K key, Object field, ProtoSchema schema, boolean writeBehind, Object... args) throws Exception {
        // 先从缓存中获取数据
        V value = doHget(key.getCacheKey(), field, schema);
        if (value != null) {
            // 数据有缓存，但其实缓存定义的空数据，
            // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
            // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
            if (!checkExist(value)) {
                return null;
            }
            statistics.addHitCount();
            return value;
        }
        statistics.addMissCount();

        // 缓存数据不存在，从Dao回源加载数据
        int index = key.getLockIndex();
        final List<V> values;
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                // 双重检测，继续判断缓存中是否存在
                value = doHget(key.getCacheKey(), field, schema);
                if (value != null) {
                    return value;
                }
                values = loader.hload(key, schema, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次读取数据库，加重数据库负担
            values = loader.hload(key, schema, this, args);
        }
        if (values == null) {
            return null;
        }
        statistics.addMissCount();
        doWriteCacheHAll(key, values, schema, writeBehind, args);
        value = doHget(key.getCacheKey(), field, schema);
        // 数据有缓存，但其实缓存定义的空数据，
        // 此时改成返回空告诉业务方是没有数据的，只是命中了缓存
        // 当该KEY新增时缓存会更新，不会导致一直缓存不存在的数据
        if (!checkExist(value)) {
            return null;
        }
        return value;
    }

    private List<V> doHgetAllCacheX(K key, ProtoSchema schema, Object... args) throws Exception {
        // 先从缓存中获取数据
        List<V> values = doHgetAll(key.getCacheKey(), schema);
        if (values != null) {
            statistics.addHitCount();
            return values;
        }
        // 缓存数据不存在，从Dao回源加载数据
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                // 双重检测，继续判断缓存中是否存在
                values = doHgetAll(key.getCacheKey(), schema, args);
                if (values != null) {
                    return values;
                }
                values = loader.hload(key, schema, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次读取数据库，加重数据库负担
            values = loader.hload(key, schema, this, args);
        }
        if (values == null) {
            return null;
        }
        doWriteCacheHAll(key, values, schema, config.isWriteBehind(), args);
        statistics.addMissCount();
        return values;
    }

    private int doHdeleteCacheX(K key, Object field, Object... args) throws Exception {
        if (key == null || field == null) {
            return -1;
        }

        // 先设置缓存过期时间，之后这么操作的原因有：
        // 1、不能简单先删除Key，因为有可能该二级缓存的数据量很大，只删除一级Key代价高
        // 2、不能简单先删除二级Key，因为有可能该二级缓存删除之后，但数据库没删除成功或者服务被重启了，缓存出现脏数据
        // 3、设置短时间的TTL即使在数据库删除成功，但下一步删除缓存服务被重启后依然可以在TTL过期之后删除，
        // 不过注意数据删除成功后还没删除缓存而服务被重启这期间可能有脏数据，只能做到数据最终一致
        int preExpire = cache.expire(key.getCacheKey(), DEAFULT_CACHE_EXPIRE_TIME);
        // 先从缓存中移除数据
        int count = -1;
        // 再从数据库中删除数据
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.hdelete(key, field, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次删除数据库
            count = loader.hdelete(key, field, this, args);
        }
        if (count == -1) {
            // 数据库删除失败，还原之前的过期时间
            cache.expire(key.getCacheKey(), preExpire);
        } else {
            // 数据库删除成功，同时删除二级缓存Key
            cache.remove(key.getCacheKey(), field.toString());
        }
        return count;
    }

    private int doHdeleteCacheX(K key, String[] fields, Object... args) throws Exception {
        if (key == null || fields == null) {
            return -1;
        }

        // 先设置缓存过期时间，之后这么操作的原因有：
        // 1、不能简单先删除Key，因为有可能该二级缓存的数据量很大，只删除一级Key代价高
        // 2、不能简单先删除二级Key，因为有可能该二级缓存删除之后，在数据库没删除成功或者服务被重启了，缓存出现脏数据
        // 3、设置短时间的TTL即使在数据库删除成功，但下一步删除缓存服务被重启后依然可以在TTL过期之后删除，
        // 不过注意数据删除成功后还没删除缓存而服务被重启这期间可能有脏数据，只能做到数据最终一致
        int preExpire = cache.expire(key.getCacheKey(), DEAFULT_CACHE_EXPIRE_TIME);
        // 再从数据库中删除数据
        int count = -1;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.hdelete(key, fields, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次删除数据库
            count = loader.hdelete(key, fields, this, args);
        }
        if (count == -1) {
            // 数据库删除失败，还原之前的过期时间
            cache.expire(key.getCacheKey(), preExpire);
        } else {
            // 数据库删除成功，同时删除二级缓存Key
            cache.remove(key.getCacheKey(), fields);
        }
        return count;
    }

    private int doHupdateCacheX(K key, Object field, V value, ProtoSchema schema, Object... args) throws Exception {
        if (key == null || field == null || value == null) {
            return -1;
        }

        // 先设置缓存过期时间，之后这么操作的原因有：
        // 1、不能简单先删除Key，因为有可能该二级缓存的数据量很大，只删除一级Key代价高
        // 2、不能简单先删除二级Key，因为有可能该二级缓存删除之后，但数据库没更新成功或者服务被重启了，缓存出现脏数据
        // 3、不能简单先更新二级Key，因为有可能该二级缓存更新之后，但数据库没更新成功或者服务被重启了，缓存出现脏数据
        // 4、设置短时间的TTL即使在数据库删除成功，但下一步删除缓存服务被重启后依然可以在TTL过期之后删除，
        // 不过注意数据更新成功后还没删除缓存而服务被重启这期间可能有脏数据，只能做到数据最终一致
        int preExpire = cache.expire(key.getCacheKey(), DEAFULT_CACHE_EXPIRE_TIME);
        // 再从数据库中更新数据
        int count = -1;
        int index = key.getLockIndex();
        if (index > 0) {
            try {
                // 加锁操作，避免同一时间有多个请求涌进
                lock.writeLock(index);
                count = loader.hupdate(key, field, value, schema, this, args);
            } finally {
                lock.writeUnlock(index);
            }
        } else {
            // 不加锁操作，可以提升性能，但多个请求进来会多次更新数据库
            count = loader.hupdate(key, field, value, schema, this, args);
        }
        if (count == -1) {
            // 数据库删除失败，还原之前的过期时间
            cache.expire(key.getCacheKey(), preExpire);
        } else {
            // 数据库删除成功，同时删除二级缓存Key
            cache.remove(key.getCacheKey(), field.toString());
        }
        return count;
    }

    /**
     * 将数据库中取出的数据写入到缓存中
     *
     * @param writeBehind 是否采用异步写，如果上层接口已经是异步了则参数为FALSE
     */
    private void doWriteCache(CacheKey<?> key, V value, ProtoSchema schema, boolean writeBehind, Object... args) {
        // 缓存异步/同步写
        if (writeBehind && threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doPut(key.getCacheKey(), value, schema, args);
                }
            });
        } else {
            doPut(key.getCacheKey(), value, schema, args);
        }
    }

    private void doWriteCacheList(CacheKey<?> key, List<V> values, ProtoSchema schema, boolean writeBehind, Object... args) {
        // 缓存异步/同步写
        if (writeBehind && threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doPutList(key.getCacheKey(), values, schema, args);
                }
            });
        } else {
            doPutList(key.getCacheKey(), values, schema, args);
        }
    }

    /**
     * 将数据库中取出的数据写入到缓存中
     */
    private void doWriteCacheH(K key, Object field, V value, ProtoSchema schema, boolean writeBehind, Object... args) {
        // 缓存异步/同步写
        if (writeBehind && threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doHput(key.getCacheKey(), field, value, schema, args);
                }
            });
        } else {
            doHput(key.getCacheKey(), field, value, schema, args);
        }
    }

    /**
     * 将数据库中取出的二级缓存数据写入到缓存中
     */
    private void doWriteCacheHAll(K key, List<V> values, ProtoSchema schema, boolean writeBehind, Object... args) {
        // 缓存异步/同步写
        if (writeBehind && threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    doHputAll(key.getCacheKey(), values, schema, args);
                }
            });
        } else {
            doHputAll(key.getCacheKey(), values, schema, args);
        }
    }

    /**
     * 从缓存中存储数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param value  存储数据
     * @param schema 数据元信息，用于序列化/反序列化
     * @return 存储成功返回true
     */
    public abstract boolean doPut(String key, V value, ProtoSchema schema, Object... args);

    /**
     * 从缓存中存储数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param values 存储数据集
     * @param schema 数据元信息，用于序列化/反序列化
     * @return 存储成功返回true
     */
    protected abstract boolean doPutList(String key, List<V> values, ProtoSchema schema, Object... args);

    /**
     * 从缓存中存储二级数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param value  存储数据
     * @param field  缓存二级Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @return 存储成功返回true
     */
    public abstract boolean doHput(String key, Object field, V value, ProtoSchema schema, Object... args);

    /**
     * 从缓存中存储二级数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param value  存储数据
     * @param schema 数据元信息，用于序列化/反序列化
     * @return 存储成功返回true
     */
    public abstract boolean doHputAll(String key, List<V> value, ProtoSchema schema, Object... args);

    /**
     * 从缓存中获取数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param schema 数据元信息，用于序列化/反序列化
     */
    public abstract V doGet(String key, ProtoSchema schema, Object... args);

    /**
     * 从缓存中获取数据集，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param schema 数据元信息，用于序列化/反序列化
     */
    public abstract List<V> doGetList(String key, ProtoSchema schema, Object... args);

    /**
     * 判断获取的数据是否为空
     *
     * @param value 通过{@link #doGet(String, ProtoSchema, Object...)}获取的数据
     */
    public abstract boolean checkExist(V value);

    /**
     * 从缓存中获取二级缓存数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param field  缓存二级Key
     * @param schema 数据元信息，用于序列化/反序列化
     */
    public abstract V doHget(String key, Object field, ProtoSchema schema, Object... args);

    /**
     * 判断获取的二级数据是否为空
     *
     * @param value 通过{@link #doHget(String, Object, ProtoSchema, Object...)}获取的数据
     */
    public abstract boolean checkHExist(V value);

    /**
     * 从缓存中获取所有二级缓存数据，由具体数据类型类实现
     *
     * @param key    存储Key
     * @param schema 数据元信息，用于序列化/反序列化
     */
    public abstract List<V> doHgetAll(String key, ProtoSchema schema, Object... args);
}
