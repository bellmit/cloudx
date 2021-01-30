package cloud.apposs.cachex.storage;

import java.util.List;

/**
 * {@link cloud.apposs.cachex.storage.Dao}数据相关操作监听器，全局单例
 */
public interface DaoListener {
    /**
     * 单条数据查询监听开始
     *
     * @param table    数据表
     * @param primary  数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param identity 数据主键值，和primary配合使用根据主键查询，可为空
     * @param query    查询条件，可为空
     */
    void selectStart(String table, String primary, Object identity, Query query);

    /**
     * 单条数据查询监听结束
     *
     * @param table    数据表
     * @param primary  数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param identity 数据主键值，和primary配合使用根据主键查询，可为空
     * @param query    查询条件，可为空
     * @param t        若查询异常，则为非空
     */
    void selectComplete(String table, String primary, Object identity, Query query, Throwable t);

    /**
     * 单条数据查询监听开始
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     */
    void selectStart(String sql, String primary);

    /**
     * 单条数据查询监听结束
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param t       若查询异常，则为非空
     */
    void selectComplete(String sql, String primary, Throwable t);

    /**
     * 数据查询监听开始
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param query   查询条件，可为空
     */
    void queryStart(String table, String primary, Query query);

    /**
     * 数据查询监听结束
     *
     * @param table   数据表
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param query   查询条件，可为空
     * @param t 数据处理异常则不为空
     */
    void queryComplete(String table, String primary, Query query, Throwable t);

    /**
     * 数据查询监听开始
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     */
    void queryStart(String sql, String primary);

    /**
     * 数据查询监听结束
     *
     * @param sql     SQL查询语句
     * @param primary 数据主键字段，后续{@link Entity}可依据此主键做删、改操作，可为空
     * @param t       若查询异常，则为非空
     */
    void queryComplete(String sql, String primary, Throwable t);

    /**
     * 数据更新监听开始
     *
     * @param table   数据表
     * @param entity 实体对象，必须存在主键
     */
    void updateStart(String table, Entity entity);

    /**
     * 数据更新监听结束
     *
     * @param table   数据表
     * @param entity 实体对象，必须存在主键
     * @param t 数据处理异常则不为空
     */
    void updateComplete(String table, Entity entity, Throwable t);

    /**
     * 数据更新监听开始
     *
     * @param table   数据表
     * @param entities 实体对象列表，每个实体对象必须存在主键
     */
    void updateStart(String table, List<Entity> entities);

    /**
     * 数据更新监听结束
     *
     * @param table   数据表
     * @param entities 实体对象列表，每个实体对象必须存在主键
     * @param t 数据处理异常则不为空
     */
    void updateComplete(String table, List<Entity> entities, Throwable t);

    /**
     * 数据更新监听开始
     *
     * @param table   数据表
     * @param updater 更新条件
     */
    void updateStart(String table, Updater updater);

    /**
     * 数据更新监听结束
     *
     * @param table   数据表
     * @param updater 更新条件
     * @param t 数据处理异常则不为空
     */
    void updateComplete(String table, Updater updater, Throwable t);

    /**
     * 数据更新监听开始
     *
     * @param sql SQL查询语句
     */
    void updateStart(String sql);

    /**
     * 数据更新监听结束
     *
     * @param sql SQL查询语句
     * @param t   若查询异常，则为非空
     */
    void updateComplete(String sql, Throwable t);

    /**
     * 数据删除监听开始
     *
     * @param table  数据表
     * @param entity 实体对象，必须存在主键
     */
    void deleteStart(String table, Entity entity);

    /**
     * 数据删除监听结束
     *
     * @param table  数据表
     * @param entity 实体对象，必须存在主键
     */
    void deleteComplete(String table, Entity entity, Throwable t);

    /**
     * 数据删除监听开始
     *
     * @param table 数据表
     */
    void deleteStart(String table, List<Entity> entities);

    /**
     * 数据删除监听结束
     *
     * @param table 数据表
     * @param t 数据处理异常则不为空
     */
    void deleteComplete(String table, List<Entity> entities, Throwable t);

    /**
     * 数据删除监听开始
     *
     * @param table    数据表
     * @param primary  表主键字段
     * @param identity 表主键值
     */
    void deleteStart(String table, String primary, Object identity);

    /**
     * 数据删除监听结束
     *
     * @param table    数据表
     * @param primary  表主键字段
     * @param identity 表主键值
     * @param t 数据处理异常则不为空
     */
    void deleteComplete(String table, String primary, Object identity, Throwable t);

    /**
     * 数据删除监听开始
     */
    void deleteStart(String table, Where where);

    /**
     * 数据删除监听结束
     *
     * @param t 数据处理异常则不为空
     */
    void deleteComplete(String table, Where where, Throwable t);

    /**
     * 数据插入监听开始
     * @param table  数据库表
     * @param entity 数据条目
     */
    void insertStart(String table, Entity entity);

    /**
     * 数据插入监听结束
     * @param table  数据库表
     * @param entity 数据条目
     * @param t 数据处理异常则不为空
     */
    void insertStart(String table, Entity entity, Throwable t);

    /**
     * 数据插入监听开始
     */
    void insertStart(String table, List<Entity> entities);

    /**
     * 数据插入监听结束
     */
    void insertStart(String table, List<Entity> entities, Throwable t);
}
