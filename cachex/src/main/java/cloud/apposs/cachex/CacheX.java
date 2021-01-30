package cloud.apposs.cachex;

import java.util.List;

import cloud.apposs.cachex.memory.CacheManager;
import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

/**
 * 数据服务接口，将Dao数据源和Cache缓存数据进行结合统一封装，
 * 内部维护着指定数据源的DAO以及指定缓存类型的CACHE，
 * 注意如果外部业务需要配置多数据源，则需要维护多种不同的源CacheX实例
 * @param <K> 缓存Key
 * @param <V> 缓存Value
 */
public interface CacheX<K extends CacheKey, V> {
    /**
     * 缓存执行异常时的返回值
     */
    public static final int CACHEX_EXECUTE_FAILURE = -1;

    /**
     * 获取Dao数据源
     */
    Dao getDao();

    /**
     * 获取指定数据源，便于支持多源开发，适用场景：
     * 1、固定数据存储用mysql源存储，文本存储用es源存储，便于文档可通过ES快速检索
     * 2、主从数据库读写分离，写用主库，读用从库，减少数据库压力，提升读性能
     *
     * @param source 配置的数据库源
     */
    Dao getDao(String source);

    /**
     * 获取缓存接口
     */
    CacheManager getCache();

    /**
     * 数据存储，对应数据获取方法为{@link #get(CacheKey, ProtoSchema, Object...)}，
     * 和{@link #hput(CacheKey, Object, Object, ProtoSchema, Object...)}方法互斥，
     * 对应数据实现接口为{@link CacheLoader#add(CacheKey, Object, ProtoSchema, CacheX, Ref, Object...)}，
     * 不能一个存储里面既能存储一级数据，又能存储二级数据，这样在同样Key的情况会有数据覆盖风险
     *
     * @param  key    存储Key
     * @param  value  存储数据
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  args   业务方传递的参数
     * @return 成功存储的数据条数，存储失败返回-1
     */
    int put(K key, V value, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 批量插入数据集
     *
     * @param  key    存储Key列表，可为空
     * @param  values 数据集
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  args   业务方传递的参数
     * @return 成功存储的数据条数，存储失败返回-1
     */
    int put(K key, List<V> values, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 基于索引获取数据，默认有缓存，检索速度最快
     * 对应数据实现接口为{@link CacheLoader#load(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key    数据Key，不允许为空
     * @param schema 数据元信息，用于序列化/反序列化
     * @param args   业务方传递的参数
     */
    V get(K key, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 通过查询条件检索单条数据，并用特别KEY进行缓存存储，
     * 对应数据实现接口为{@link CacheLoader#select(CacheKey, ProtoSchema, Query, CacheX, Object...)}
     * 一般应用于复杂条件单条数据查询并且注意要确保要查询的数据是唯一，底层会只查询一条
     * 注意一般情况下如果有索引一定要改成{@link #get(CacheKey, ProtoSchema, Object...)}来进行基于索引的查询
     *
     * @param  key    存储Key，因为每个复杂查询Key不同，就不限制Key的主键定义，允许为空，为空则不缓存
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  query  查询条件，可为空
     * @param  args   业务方传递的参数
     * @return 查询结果集合
     */
    V select(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception;

    /**
     * 通过查询条件检索所有数据集，并用特别KEY进行缓存存储
     * 对应数据实现接口为{@link CacheLoader#query(CacheKey, ProtoSchema, Query, AbstractCacheX, Object...)}
     *
     * @param  key    存储Key，因为每个复杂查询Key不同，就不限制Key的主键定义，允许为空，为空则不缓存
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  query  查询条件，可为空
     * @param  args   业务方传递的参数
     * @return 查询结果集合
     */
    List<V> query(CacheKey<?> key, ProtoSchema schema, Query query, Object... args) throws Exception;

    /**
     * 检查指定Key的数据是否存在，底层依然是获取指定数据判断是否为空来判断数据是否存在，
     * 对应数据实现接口为{@link CacheLoader#load(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key    数据Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @param args   业务方传递的参数
     */
    boolean exist(K key, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 检查指定Key的数据是否存在，底层依然是获取指定数据判断是否为空来判断数据是否存在，
     * 当数据为多条时，只要有一条数据存在则代表数据存在
     * 对应数据实现接口为{@link CacheLoader#query(CacheKey, ProtoSchema, Query, AbstractCacheX, Object...)}
     *
     * @param key    数据Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @param query  查询条件，可为空
     * @param args   业务方传递的参数
     */
    boolean exist(K key, ProtoSchema schema, Query query, Object... args) throws Exception;

    /**
     * 删除数据，
     * 对应数据实现接口为{@link CacheLoader#delete(CacheKey, CacheX, Object...)}
     *
     * @param  key  数据Key
     * @param  args 业务方传递的参数
     * @return 成功删除的数据条数，删除失败返回-1
     */
    int delete(K key, Object... args) throws Exception;

    /**
     * 数据更新，
     * 对应数据实现接口为{@link CacheLoader#update(CacheKey, Object, ProtoSchema, CacheX, Object...)}
     *
     * @param  key    存储Key
     * @param  value  存储数据
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  args   业务方传递的参数
     * @return 成功更新的数据条数，更新失败返回-1
     */
    int update(K key, V value, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 二级Key数据存储，对应数据获取方法为{@link #hget(CacheKey, Object, ProtoSchema, Object...)}，
     * 和{@link #put(CacheKey, Object, ProtoSchema, Object...)}方法互斥，
     * 不能一个存储里面既能存储一级数据，又能存储二级数据，这样在同样Key的情况会有数据覆盖风险，
     * 对应数据实现接口为{@link CacheLoader#hadd(CacheKey, Object, Object, ProtoSchema, CacheX, Ref, Object...)}
     *
     * @param  key    存储Key
     * @param  field  缓存二级Key
     * @param  value  存储数据
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  args   业务方传递的参数
     * @return 成功存储的数据条数，存储失败返回-1
     */
    int hput(K key, Object field, V value, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 获取二级数据，
     * 对应数据实现接口为{@link CacheLoader#hload(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key    数据Key
     * @param field  缓存二级Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @param args   业务方传递的参数
     */
    V hget(K key, Object field, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 检查指定二级Key的数据是否存在，诡异依然是获取指定数据判断是否为空来判断数据是否存在，
     * 对应数据实现接口为{@link CacheLoader#load(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key    数据Key
     * @param field  缓存二级Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @param args   业务方传递的参数
     */
    boolean hexist(K key, Object field, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 获取所有二级数据，注意如果数据量过多会有OOM风险，
     * 对应数据实现接口为{@link CacheLoader#hload(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key    数据Key
     * @param schema 数据元信息，用于序列化/反序列化
     * @param args   业务方传递的参数
     */
    List<V> hgetAll(K key, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 删除二级数据，
     * 对应数据实现接口为{@link CacheLoader#hdelete(CacheKey, Object, CacheX, Object...)}
     *
     * @param key   数据Key
     * @param field 缓存二级Key
     * @param args  业务方传递的参数
     */
    int hdelete(K key, Object field, Object... args) throws Exception;

    /**
     * 批量删除二级数据，
     * 对应数据实现接口为{@link CacheLoader#hdelete(CacheKey, Object[], CacheX, Object...)}
     *
     * @param key   数据Key
     * @param fields 缓存二级Key
     * @param args   业务方传递的参数
     */
    int hdelete(K key, Object[] fields, Object... args) throws Exception;

    /**
     * 数据更新，
     * 对应数据实现接口为{@link CacheLoader#hupdate(CacheKey, Object, Object, ProtoSchema, CacheX, Object...)}
     *
     * @param  key    存储Key
     * @param  field  缓存二级Key
     * @param  value  存储数据
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  args   业务方传递的参数
     * @return 更新成功返回true
     */
    int hupdate(K key, Object field, V value, ProtoSchema schema, Object... args) throws Exception;

    /**
     * 异步获取数据，底层采用线程池异步执行，
     * 对应数据实现接口为{@link CacheLoader#load(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncGet(K key, ProtoSchema schema, AsyncCacheXHandler<V> handler, Object... args);

    /**
     * 异步获取数据，底层采用线程池异步执行，
     * 对应数据实现接口为{@link CacheLoader#select(CacheKey, ProtoSchema, Query, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param schema  数据元信息，用于序列化/反序列化
     * @param query   查询条件，可为空
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncSelect(K key, ProtoSchema schema, Query query, AsyncCacheXHandler<V> handler, Object... args);

    /**
     * 通过查询条件异步获取数据，并用特别KEY进行缓存存储
     *
     * @param  key    存储Key，因为每个复杂查询Key不同，就不限制Key的主键定义
     * @param  schema 数据元信息，用于序列化/反序列化
     * @param  query  查询条件，可为空
     * @param  handler 异步回调函数
     * @param  args   业务方传递的参数
     * @return 查询结果集合
     */
    void asyncQuery(CacheKey<?> key, ProtoSchema schema,
                       Query query, AsyncCacheXHandler<List<V>> handler, Object... args) throws Exception;

    /**
     * 异步数据存储，底层采用线程池异步执行，
     * 对应数据获取方法为{@link #get(CacheKey, ProtoSchema, Object...)}，
     * 和{@link #hput(CacheKey, Object, Object, ProtoSchema, Object...)}方法互斥，
     * 对应数据实现接口为{@link CacheLoader#add(CacheKey, Object, ProtoSchema, CacheX, Ref, Object...)}
     *
     * @param key     存储Key
     * @param value   存储数据
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncPut(K key, V value, ProtoSchema schema, AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 异步删除数据，底层采用线程池异步执行，
     * 对应数据实现接口为{@link CacheLoader#delete(CacheKey, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asynDelete(K key, AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 异步数据更新，底层采用线程池异步执行，
     * 对应数据实现接口为{@link CacheLoader#update(CacheKey, Object, ProtoSchema, CacheX, Object...)}
     *
     * @param key     存储Key
     * @param value   存储数据
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncUpdate(K key, V value, ProtoSchema schema,
                     AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 二级Key数据异步存储，对应数据获取方法为{@link #hget(CacheKey, Object, ProtoSchema, Object...)}，
     * 和{@link #put(CacheKey, Object, ProtoSchema, Object...)}方法互斥，
     * 不能一个存储里面既能存储一级数据，又能存储二级数据，这样在同样Key的情况会有数据覆盖风险，
     * 对应数据实现接口为{@link CacheLoader#hadd(CacheKey, Object, Object, ProtoSchema, CacheX, Ref, Object...)}
     *
     * @param key     存储Key
     * @param field   缓存二级Key
     * @param value   存储数据
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHput(K key, Object field, V value, ProtoSchema schema,
                   AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 异步获取二级数据，
     * 对应数据实现接口为{@link CacheLoader#hload(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param field   缓存二级Key
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHget(K key, Object field, ProtoSchema schema,
                   AsyncCacheXHandler<V> handler, Object... args);

    /**
     * 异步获取所有二级数据，注意如果数据量过多会有OOM风险，
     * 对应数据实现接口为{@link CacheLoader#hload(CacheKey, ProtoSchema, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHgetAll(K key, ProtoSchema schema,
                      AsyncCacheXHandler<List<V>> handler, Object... args);

    /**
     * 删除二级数据，
     * 对应数据实现接口为{@link CacheLoader#hdelete(CacheKey, Object, CacheX, Object...)}
     *
     * @param key     数据Key
     * @param field   缓存二级Key
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHdelete(K key, Object field, AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 批量删除二级数据，
     * 对应数据实现接口为{@link CacheLoader#hdelete(CacheKey, Object[], CacheX, Object...)}
     *
     * @param key     数据Key
     * @param fields  缓存二级Key
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHdelete(K key, Object[] fields, AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 数据更新，
     * 对应数据实现接口为{@link CacheLoader#hupdate(CacheKey, Object, Object, ProtoSchema, CacheX, Object...)}
     *
     * @param key     存储Key
     * @param field   缓存二级Key
     * @param value   存储数据
     * @param schema  数据元信息，用于序列化/反序列化
     * @param handler 异步回调函数
     * @param args    业务方传递的参数
     */
    void asyncHupdate(K key, Object field, V value, ProtoSchema schema,
                      AsyncCacheXHandler<Integer> handler, Object... args);

    /**
     * 缓存统计服务
     */
    CacheXStatistics getStatistics();

    /**
     * 关闭服务，释放资源
     */
    void shutdown();
}
