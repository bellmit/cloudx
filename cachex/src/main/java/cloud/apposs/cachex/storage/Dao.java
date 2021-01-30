package cloud.apposs.cachex.storage;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;
import cloud.apposs.util.StrUtil;

import java.util.List;

/**
 * 数据访问接口，为不同的数据库存储提供统一的接口调用，
 * 服务于业务方调用，全局单例，
 * 真正的数据增删改查均是通过{@link SqlBuilder}实现，新增新的数据库支持需要做以下几个步骤，
 * <pre>
 * 1、实现连接池以提升性能
 * 2、实现{@link SqlBuilder}接口，包括对{@link Query}/{@link Updater}/{@link Where}/{@link Pager}的解析细节
 * </pre>
 */
public class Dao {
    /**
     * 数据库配置
     */
    private final CacheXConfig config;

    /**
     * 真正的数据增删改查实现类
     */
    private final SqlBuilder builder;

    /**
     * 数据库操作监听列表
     */
    private final DaoListenerSupport listenerSupport = new DaoListenerSupport();

    public Dao(CacheXConfig config) {
        this.config = config;
        try {
            this.builder = SqlBuilderFactory.getSqlBuilder(config);
        } catch (Exception e) {
            throw new RuntimeException("sql builder initial fail", e);
        }
    }

    public CacheXConfig getConfig() {
        return config;
    }

    /**
     * 获取真正的接口实现类，
     * 仅用于调试输出SQL语句等
     */
    public SqlBuilder getBuilder() {
        return builder;
    }

    /**
     * 添加DAO数据操作监听，可以用于慢日志查询监控等
     */
    public void addListener(DaoListener listener) {
        listenerSupport.addListener(listener);
    }

    /**
     * 数据查询，只查询一条数据
     *
     * @param table 数据表
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table) throws Exception {
        return select(table, null, null, null, null);
    }

    /**
     * 数据查询，只查询一条数据
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, String primary) throws Exception {
        return select(table, primary, null, null, null);
    }

    /**
     * 数据查询，只查询一条数据
     *
     * @param table 数据表
     * @param query 查询条件，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, Query query) throws Exception {
        return select(table, null, null, null, query);
    }

    /**
     * 数据查询，只查询一条数据
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param query   查询条件，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, String primary, Query query) throws Exception {
        return select(table, primary, null, null, query);
    }

    /**
     * 数据查询，只查询一条数据，采用主键查询
     *
     * @param table    数据表
     * @param primary  数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param identity 数据主键值，和primary配合使用根据主键查询，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, String primary, Object identity) throws Exception {
        return select(table, primary, identity, null, null);
    }

    /**
     * 数据查询，只查询一条数据，主要用于HBase RowKey查询
     *
     * @param table    数据表
     * @param identity 数据主键值，和primary配合使用根据主键查询，可为空
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, Object identity, ProtoSchema schema) throws Exception {
        return select(table, null, identity, schema, null);
    }

    /**
     * 数据查询，只查询一条数据
     *
     * @param table    数据表
     * @param primary  数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param identity 数据主键值，和primary配合使用根据主键查询，可为空
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @param query    查询条件，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity select(String table, String primary,
                         Object identity, ProtoSchema schema, Query query) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }

        try {
            listenerSupport.selectStart(table, primary, identity, query);
            Entity entity = builder.select(table, primary, identity, schema, query);
            listenerSupport.selectComplete(table, primary, identity, query, null);
            return entity;
        } catch (Exception e) {
            listenerSupport.selectComplete(table, primary, identity, query, e);
            throw e;
        }
    }

    /**
     * 数据查询，查询表所有数据，
     * 注意表数据量如果很多的话会有OOM爆内存风险，建议任何查询都采用分页查询
     *
     * @param table 数据表
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> query(String table) throws Exception {
        return query(table, null, null, null);
    }

    /**
     * 数据查询，查询表所有数据，
     * 注意表数据量如果很多的话会有OOM爆内存风险，建议任何查询都采用分页查询
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> query(String table, String primary) throws Exception {
        return query(table, primary, null, null);
    }

    /**
     * 数据查询，根据查询条件查询表所有数据
     */
    public List<Entity> query(String table, Query query) throws Exception {
        return query(table, null, null, query);
    }

    /**
     * 数据查询，根据查询条件查询表所有数据
     *
     * @param table  数据表
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @param query  查询条件，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> query(String table, ProtoSchema schema, Query query) throws Exception {
        return query(table, null, schema, query);
    }

    /**
     * 数据查询，根据查询条件查询表所有数据
     *
     * @param table  数据表
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> query(String table, ProtoSchema schema) throws Exception {
        return query(table, null, schema, null);
    }

    /**
     * 数据查询，查询所有匹配条件的数据
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param schema  数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @param query   查询条件，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> query(String table, String primary, ProtoSchema schema, Query query) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }

        try {
            listenerSupport.queryStart(table, primary, query);
            List<Entity> entities = builder.query(table, primary, schema, query);
            listenerSupport.queryComplete(table, primary, query, null);
            return entities;
        } catch (Exception e) {
            listenerSupport.queryComplete(table, primary, query, e);
            throw e;
        }
    }

    /**
     * 数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table  数据表
     * @param entity 实体对象，必须存在主键
     * @return 成功更新的数据条数，更新失败返回-1
     */
    public int update(String table, Entity entity) throws Exception {
        return update(table, entity, null);
    }

    /**
     * 数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table  数据表
     * @param entity 实体对象，必须存在主键
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @return 成功更新的数据条数，更新失败返回-1
     */
    public int update(String table, Entity entity, ProtoSchema schema) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (entity == null || entity.isEmpty()) {
            throw new IllegalArgumentException("entity");
        }
        // Entity必须要有主键来做WHERE范围判断，不然一不小心更新全部就GG了
        if (StrUtil.isEmpty(entity.getPrimary()) || entity.getIdentity() == null) {
            throw new IllegalArgumentException("Entity has no identity");
        }

        try {
            listenerSupport.updateStart(table, entity);
            int count = builder.update(table, entity, schema);
            listenerSupport.updateComplete(table, entity, null);
            return count;
        } catch (Exception e) {
            listenerSupport.updateComplete(table, entity, e);
            throw e;
        }
    }

    /**
     * 数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table    数据表
     * @param entities 实体对象列表，每个实体对象必须存在主键
     * @return 成功更新数据条数，更新失败返回-1
     */
    public int update(String table, List<Entity> entities) throws Exception {
        return update(table, entities, null);
    }

    /**
     * 数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table    数据表
     * @param entities 实体对象列表，每个实体对象必须存在主键
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @return 成功更新数据条数，更新失败返回-1
     */
    public int update(String table, List<Entity> entities, ProtoSchema schema) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("entities");
        }
        // Entity必须要有主键来做WHERE范围判断，不然一不小心更新全部就GG了
        String primary = entities.get(0).getPrimary();
        for (Entity entity : entities) {
            if (StrUtil.isEmpty(entity.getPrimary()) ||
                    !primary.equals(entity.getPrimary())) {
                throw new IllegalArgumentException("Entity Primary No The Same");
            }
            if (entity.getIdentity() == null) {
                throw new IllegalArgumentException("Entity Has No Identity");
            }
        }

        try {
            listenerSupport.updateStart(table, entities);
            int count = builder.update(table, entities, schema);
            listenerSupport.updateComplete(table, entities, null);
            return count;
        } catch (Exception e) {
            listenerSupport.updateComplete(table, entities, e);
            throw e;
        }
    }

    /**
     * 数据更新，根据{@link Updater}条件更新
     *
     * @return 成功更新数据条数，更新失败返回-1
     */
    public int update(String table, Updater updater) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (updater == null || updater.isEmpty()) {
            throw new IllegalArgumentException("updater");
        }
        // Updater必须要有WHERE范围判断，不然一不小心更新全部就GG了
        if (updater.where() == null || updater.where().isEmpty()) {
            throw new IllegalArgumentException("Where Not Specified");
        }

        try {
            listenerSupport.updateStart(table, updater);
            int count = builder.update(table, updater);
            listenerSupport.updateComplete(table, updater, null);
            return count;
        } catch (Exception e) {
            listenerSupport.updateComplete(table, updater, e);
            throw e;
        }
    }

    /**
     * 数据删除，根据{@link Entity}主键删除，注意主键不能为空
     *
     * @param  table  数据库表
     * @param  entity 实体对象，必须存在主键
     * @return 成功删除返回true
     */
    public int delete(String table, Entity entity) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        // Entity必须要有主键来做WHERE范围判断，不然一不小心删除全部就GG了
        if (StrUtil.isEmpty(entity.getPrimary()) || entity.getIdentity() == null) {
            throw new IllegalArgumentException("Entity Has No Identity");
        }

        try {
            listenerSupport.deleteStart(table, entity);
            int count = builder.delete(table, entity);
            listenerSupport.deleteComplete(table, entity, null);
            return count;
        } catch (Exception e) {
            listenerSupport.deleteComplete(table, entity, e);
            throw e;
        }
    }

    /**
     * 数据删除，根据主键删除
     *
     * @param table    数据库表
     * @param primary  表主键字段
     * @param identity 表主键值
     * @return 成功删除的数据条数
     */
    public int delete(String table, String primary, Object identity) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        // Entity必须要有主键来做WHERE范围判断，不然一不小心删除全部就GG了
        if (StrUtil.isEmpty(primary) || identity == null) {
            throw new IllegalArgumentException("Entity Has No Identity");
        }

        try {
            listenerSupport.deleteStart(table, primary, identity);
            int count = builder.delete(table, primary, identity);
            listenerSupport.deleteComplete(table, primary, identity, null);
            return count;
        } catch (Exception e) {
            listenerSupport.deleteComplete(table, primary, identity, e);
            throw e;
        }
    }

    /**
     * 批量数据删除，根据{@link Entity}主键删除，注意主键不能为空
     *
     * @return 成功删除数据条数，删除失败返回-1
     */
    public int delete(String table, List<Entity> entities) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("entities");
        }
        String primary = entities.get(0).getPrimary();
        // Entity必须要有主键来做WHERE范围判断，不然一不小心删除全部就GG了
        for (Entity entity : entities) {
            if (StrUtil.isEmpty(entity.getPrimary()) ||
                    !primary.equals(entity.getPrimary())) {
                throw new IllegalArgumentException("Entity Primary No The Same");
            }
            if (entity.getIdentity() == null) {
                throw new IllegalArgumentException("Entity Has No Identity");
            }
        }

        try {
            listenerSupport.deleteStart(table, primary, entities);
            int count = builder.delete(table, entities);
            listenerSupport.deleteComplete(table, primary, entities, null);
            return count;
        } catch (Exception e) {
            listenerSupport.deleteComplete(table, primary, entities, e);
            throw e;
        }
    }

    /**
     * 数据删除，根据{@link Where}条件删除
     *
     * @return 成功删除数据条数，删除失败返回-1
     */
    public int delete(String table, Where where) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        // 必须要有WHERE范围判断，不然一不小心删除全部就GG了
        if (where == null || where.isEmpty()) {
            throw new IllegalArgumentException("'Where' parameter required");
        }

        try {
            listenerSupport.deleteStart(table, where);
            int count = builder.delete(table, where);
            listenerSupport.deleteComplete(table, where, null);
            return count;
        } catch (Exception e) {
            listenerSupport.deleteComplete(table, where, e);
            throw e;
        }
    }

    /**
     * 数据插入
     *
     * @param table  数据库表
     * @param entity 数据条目
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, Entity entity) throws Exception {
        return insert(table, entity, null, null);
    }

    /**
     * 数据插入
     *
     * @param table  数据库表
     * @param entity 数据条目
     * @param idRef  是否获取生成的ID，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, Entity entity, Ref<Object> idRef) throws Exception {
        return insert(table, entity, null, idRef);
    }

    /**
     * 数据插入
     *
     * @param table  数据库表
     * @param entity 数据条目
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @param idRef  是否获取生成的ID，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, Entity entity, ProtoSchema schema, Ref<Object> idRef) throws Exception {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }

        try {
            listenerSupport.insertStart(table, entity);
            int count = builder.insert(table, entity, schema, idRef);
            listenerSupport.insertComplete(table, entity, null);
            return count;
        } catch (Exception e) {
            listenerSupport.insertComplete(table, entity, e);
            throw e;
        }
    }

    /**
     * 数据批量插入
     *
     * @param table    数据库表
     * @param entities 数据条目列表
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, List<Entity> entities) throws Exception {
        return insert(table, entities, null, null);
    }

    /**
     * 数据批量插入
     *
     * @param table    数据库表
     * @param entities 数据条目列表
     * @param idList   是否获取生成的ID列表，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, List<Entity> entities, List<Object> idList) throws Exception {
        return insert(table, entities, null, idList);
    }

    /**
     * 数据批量插入
     *
     * @param table    数据库表
     * @param entities 数据条目列表
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, List<Entity> entities, ProtoSchema schema) throws Exception {
        return insert(table, entities, schema, null);
    }

    /**
     * 数据批量插入
     *
     * @param table    数据库表
     * @param entities 数据条目列表
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @param idList   是否获取生成的ID列表，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    public int insert(String table, List<Entity> entities,
                      ProtoSchema schema, List<Object> idList) throws Exception {
        try {
            listenerSupport.insertStart(table, entities);
            int count = builder.insert(table, entities, schema, idList);
            listenerSupport.insertComplete(table, entities, null);
            return count;
        } catch (Exception e) {
            listenerSupport.insertComplete(table, entities, e);
            throw e;
        }
    }

    /**
     * SQL数据查询，只返回第一条数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql SQL查询语句
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity executeSelect(String sql) throws Exception {
        return executeSelect(sql, null);
    }

    /**
     * SQL数据查询，只返回第一条数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    public Entity executeSelect(String sql, String primary) throws Exception {
        try {
            listenerSupport.selectStart(sql, primary);
            Entity entity = builder.executeSelect(sql, primary);
            listenerSupport.selectComplete(sql, primary, null);
            return entity;
        } catch (Exception e) {
            listenerSupport.selectComplete(sql, primary, e);
            throw e;
        }
    }

    /**
     * SQL数据查询，查询所有匹配条件的数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql SQL查询语句
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> executeQuery(String sql) throws Exception {
        return executeQuery(sql, null);
    }

    /**
     * SQL数据查询，查询所有匹配条件的数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    public List<Entity> executeQuery(String sql, String primary) throws Exception {
        try {
            listenerSupport.selectStart(sql, primary);
            List<Entity> entities = builder.executeQuery(sql, primary);
            listenerSupport.selectComplete(sql, primary, null);
            return entities;
        } catch (Exception e) {
            listenerSupport.selectComplete(sql, primary, e);
            throw e;
        }
    }

    /**
     * 数据更新，根据SQL语句更新，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @return 成功更新数据条数，更新失败返回-1
     */
    public int executeUpdate(String sql) throws Exception {
        try {
            listenerSupport.updateStart(sql);
            int count = builder.executeUpdate(sql);
            listenerSupport.updateComplete(sql, null);
            return count;
        } catch (Exception e) {
            listenerSupport.updateComplete(sql, e);
            throw e;
        }
    }

    /**
     * 创建数据表
     *
     * @param metadata 表格元信息
     */
    public boolean create(Metadata metadata) throws Exception {
        return create(metadata, false);
    }

    /**
     * 创建数据表
     *
     * @param metadata    表格元信息
     * @param dropIfExist 当表格已经存在是否删除
     * @return 创建成功返回true
     */
    public boolean create(Metadata metadata, boolean dropIfExist) throws Exception {
        if (metadata == null || StrUtil.isEmpty(metadata.getTable())) {
            throw new IllegalArgumentException("metadata");
        }

        return builder.create(metadata, dropIfExist);
    }

    /**
     * 数据数据表是否已经存在
     *
     * @param metadata 表格元信息
     */
    public boolean exist(Metadata metadata) throws Exception {
        if (metadata == null || StrUtil.isEmpty(metadata.getTable())) {
            throw new IllegalArgumentException("metadata");
        }

        return builder.exist(metadata);
    }

    /**
     * 获取接口层内部是用何种方言实现
     */
    public String getDialect() {
        return builder.getDialect();
    }

    public synchronized void shutdown() {
        builder.shutdown();
    }
}
