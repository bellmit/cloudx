package cloud.apposs.cachex;

import java.util.List;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

/**
 * 数据加载器，
 * 因为数据如何加载只有业务清楚，所以由业务自己实现对数据的加载，
 * 具体有如下使用场景：
 * 1、当没命中缓存时调用该服务从数据库加载数据到缓存中
 * 2、当添加数据时存储该数据到数据库中
 * 3、当删除数据时从数据库删除数据
 * 4、当更新数据时从数据库更新数据
 */
public interface CacheLoader<K extends CacheKey, V> {
    /**
     * 初始化数据加载器，{@link CacheX}创建时便会自动初始化，
     * 默认不实现，可实现数据的启动加载，提升服务性能
     *
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象，
     *               注意所有的数据必须要调用CacheX进行数据存储以便于底层能够获取到数据
     */
    void initialize(CacheX<K, V> cachex);

    /**
     * 通过索引从数据库中加载数据并添加到缓存中，
     * 和{@link CacheX#get(CacheKey, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象，
     *               注意所有的数据必须要调用CacheX进行数据存储以便于底层能够获取到数据
     * @param args   业务方传递的参数
     * @return Key对应的加载数据
     */
    V load(K key, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 通过业务传递的查询条件从数据库中加载数据并添加到缓存中，
     * 和{@link CacheX#select(CacheKey, ProtoSchema, Query, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param query  查询条件
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象，
     *               注意所有的数据必须要调用CacheX进行数据存储以便于底层能够获取到数据
     * @param args   业务方传递的参数
     * @return Key对应的加载数据
     */
    V select(CacheKey<?> key, ProtoSchema schema, Query query, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 通过业务传递的查询条件从数据库中批量加载数据并添加到缓存中，
     * 和{@link CacheX#query(CacheKey, ProtoSchema, Query, Object...)} 方法对应
     *
     * @param key    缓存Key
     * @param query  查询条件
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象，
     *               注意所有的数据必须要调用CacheX进行数据存储以便于底层能够获取到数据
     * @param args   业务方传递的参数
     * @return Key对应的加载数据
     */
    List<V> query(CacheKey<?> key, ProtoSchema schema,
                  Query query, AbstractCacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 添加数据到数据库中并添加到缓存中，
     * 和{@link CacheX#put(CacheKey, Object, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param value  缓存数据
     * @param cachex 缓存服务，
     *               因为缓存可以存储String、对象等，需要让业务方自己实现存储String或者对象
     * @param idRef  数据存储后生成的主键ID，如果返回非空则会设置到{@link K#setPrimary(Object)}中
     * @param args   业务方传递的参数
     * @return 添加成功的数据条数
     */
    int add(K key, V value, ProtoSchema schema, CacheX<K, V> cachex, Ref<Object> idRef, Object... args) throws Exception;

    /**
     * 批量添加数据到数据库中并添加到缓存中，
     * 和{@link CacheX#put(CacheKey, Object, ProtoSchema, Object...)}方法对应
     *
     * @param values 存储的数据集
     * @param cachex 缓存服务，
     *               因为缓存可以存储String、对象等，需要让业务方自己实现存储String或者对象
     * @param idRefs 数据存储后生成的主键ID，如果返回非空则会设置到{@link K#setPrimary(Object)}中
     * @param args   业务方传递的参数
     * @return 添加成功的数据条数
     */
    int add(List<V> values, ProtoSchema schema, CacheX<K, V> cachex, List<Object> idRefs, Object... args) throws Exception;

    /**
     * 删除数据，
     * 和{@link CacheX#delete(CacheKey, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param cachex 缓存服务，
     *               因为缓存可以存储String、对象等，需要让业务方自己实现删除String或者对象
     * @param args   业务方传递的参数
     * @return 删除成功的数据条数
     */
    int delete(K key, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 从数据库中更新数据，
     * 和{@link CacheX#update(CacheKey, Object, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param value  更新数据
     * @param cachex 缓存服务
     * @param args   业务方传递的参数
     * @return 更新成功的数据条数
     */
    int update(K key, V value, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 从数据库中加载所有二级数据并添加到缓存中，
     * 和{@link CacheX#hget(CacheKey, Object, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象，
     *               注意所有的数据必须要调用CacheX进行数据存储以便于底层能够获取到数据
     * @param args   业务方传递的参数
     * @return Key对应的加载数据
     */
    List<V> hload(K key, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 添加数据到数据库中并添加到缓存中，
     * 和{@link CacheX#hput(CacheKey, Object, Object, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param field  二级缓存Key
     * @param value  缓存数据
     * @param cachex 缓存服务
     * @param idRef  数据存储后生成的主键ID，如果返回非空则会设置到{@link K#setPrimary(Object)}中
     * @param args   业务方传递的参数
     * @return 添加成功的数据条数
     */
    int hadd(K key, Object field, V value, ProtoSchema schema,
             CacheX<K, V> cachex, Ref<Object> idRef, Object... args) throws Exception;

    /**
     * 删除二级缓存数据，
     * 和{@link CacheX#hdelete(CacheKey, Object, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param field  二级缓存Key
     * @param cachex 缓存服务，
     *               因为缓存可以存储String、对象等，需要让业务方自己实现删除String或者对象
     * @param args   业务方传递的参数
     * @return 删除成功的数据条数
     */
    int hdelete(K key, Object field, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 批量删除二级缓存数据，
     * 和{@link CacheX#hdelete(CacheKey, Object, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param fields 二级缓存Key
     * @param cachex 缓存服务
     * @param args   业务方传递的参数
     * @return 删除成功的数据条数
     */
    int hdelete(K key, Object[] fields, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 从数据库中更新数据，
     * 和{@link CacheX#hupdate(CacheKey, Object, Object, ProtoSchema, Object...)}方法对应
     *
     * @param key    缓存Key
     * @param field  二级缓存Key
     * @param value  更新数据
     * @param cachex 缓存服务，
     *               因为缓存可以获取String、对象等，需要让业务方自己实现存储String或者对象
     * @param args   业务方传递的参数
     * @return 更新成功的数据条数
     */
    int hupdate(K key, Object field, V value, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception;

    /**
     * 生成业务二级缓存Key，
     * 主要场景为{@link #hload(CacheKey, ProtoSchema, CacheX, Object...)}加载所有数据后，
     * 需要自定义业务方自己的二级缓存Key
     */
    String getField(V info);
}
