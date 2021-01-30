package cloud.apposs.cachex.storage;

import java.util.List;

import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

/**
 * 数据查询接口封装，不同数据库语言其封装的SQL查询不一样，全局单例
 */
public interface SqlBuilder {
    public static final String DIALECT_MYSQL = "MySQL";
    public static final String DIALECT_ORACLE = "Oracle";
    public static final String DIALECT_SQLITE = "Sqlite";
    public static final String DIALECT_HBASE = "HBase";
    public static final String DIALECT_ELASTICSEARCH = "ElasticSearch";

    /**
     * JDBC驱动配置
     */
    public static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
    public static final String DRIVER_SQLITE = "org.sqlite.JDBC";

    /**
     * 数据库方言
     */
    String getDialect();

    /**
     * 数据查询，只查询一条数据
     *
     * @param table    数据表
     * @param primary  数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param identity 查询主键值，即通过传递的ID进行数据查询，JDBC需要与primary配合使用
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @param query    查询条件，可为空
     * @return 实体对象，以Key->Value结构存储
     */
    Entity select(String table, String primary, Object identity,
                  ProtoSchema schema, Query query) throws Exception;

    /**
     * 数据查询，查询所有匹配条件的数据
     *
     * @param table   数据表，不可为空
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param schema  数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化，可为空
     * @param query   查询条件，可为空
     * @return 实体对象集合，以Key->Value结构存储
     */
    List<Entity> query(String table, String primary,
                       ProtoSchema schema, Query query) throws Exception;

    /**
     * 数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table  数据表
     * @param entity 实体对象，必须存在主键
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @return 成功更新的数据条数，更新失败返回-1
     */
    int update(String table, Entity entity, ProtoSchema schema) throws Exception;

    /**
     * 批量数据更新，根据{@link Entity}主键更新，注意主键不能为空
     *
     * @param table    数据表
     * @param entities 实体对象列表，每个实体对象必须存在主键
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @return 成功更新数据条数，更新失败返回-1
     */
    int update(String table, List<Entity> entities, ProtoSchema schema) throws Exception;

    /**
     * 数据更新，根据{@link Updater}条件更新
     *
     * @param  table   数据表
     * @param  updater 更新条件
     * @return 成功更新数据条数，更新失败返回-1
     */
    int update(String table, Updater updater) throws Exception;

    /**
     * 数据插入
     *
     * @param table  数据库表
     * @param entity 数据条目
     * @param schema 数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @param idRef  是否获取生成的ID，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    int insert(String table, Entity entity,
               ProtoSchema schema, Ref<Object> idRef) throws Exception;

    /**
     * 数据批量插入
     *
     * @param table    数据库表
     * @param entities 数据条目列表
     * @param schema   数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
     * @param idList   是否获取生成的ID列表，为NULL则不获取
     * @return 成功插入数据条数，插入失败返回-1
     */
    int insert(String table, List<Entity> entities,
               ProtoSchema schema, List<Object> idList) throws Exception;

    /**
     * 数据删除，根据{@link Entity}主键删除，注意主键不能为空
     *
     * @return 成功删除数据条数，删除失败返回-1
     */
    int delete(String table, Entity entity) throws Exception;

    /**
     * 数据删除，根据主键删除
     *
     * @param table    数据库表
     * @param primary  表主键字段
     * @param identity 表主键值
     * @return 成功删除数据条数，删除失败返回-1
     */
    int delete(String table, String primary, Object identity) throws Exception;

    /**
     * 批量数据删除，根据{@link Entity}主键删除，注意主键不能为空
     *
     * @return 成功删除数据条数，删除失败返回-1
     */
    int delete(String table, List<Entity> entities) throws Exception;

    /**
     * 数据删除，根据{@link Where}条件删除
     *
     * @return 成功删除数据条数，删除失败返回-1
     */
    int delete(String table, Where where) throws Exception;

    /**
     * SQL数据查询，只返回第一条数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param args 附加参数，例如EsBuilder就需求额外参数请求
     * @return 实体对象，以Key->Value结构存储
     */
    Entity executeSelect(String sql, String primary, Object... args) throws Exception;

    /**
     * SQL数据查询，查询所有匹配条件的数据，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param args 附加参数，例如EsBuilder就需求额外参数请求
     * @return 实体对象集合，以Key->Value结构存储
     */
    List<Entity> executeQuery(String sql, String primary, Object... args) throws Exception;

    /**
     * 数据更新，根据SQL语句更新，
     * 注意执行此方法即表示该环境已经和指定的SQL服务绑定，无法做到动态更换不同的SQL服务
     *
     * @param sql SQL查询语句
     * @param args 附加参数，例如EsBuilder就需求额外参数请求
     * @return 成功更新数据条数，更新失败返回-1
     */
    int executeUpdate(String sql, Object... args) throws Exception;

    /**
     * 创建数据表
     *
     * @param metadata    表格元信息
     * @param dropIfExist 当表格已经存在是否删除
     * @return 创建成功返回true
     */
    boolean create(Metadata metadata, boolean dropIfExist) throws Exception;

    /**
     * 数据数据表是否已经存在
     *
     * @param metadata 表格元信息
     */
    boolean exist(Metadata metadata) throws Exception;

    /**
     * 关闭数据库连接，释放资源
     */
    void shutdown();
}
