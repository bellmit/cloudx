package cloud.apposs.cachex.storage;

import cloud.apposs.protobuf.ProtoField;
import cloud.apposs.protobuf.ProtoSchema;

/**
 * 数据库查询语句封装，支持数据库的SELECT/UPDATE等操作
 */
public class Query {
    public static final String DEFAULT_FIELDS = "*";

    /**
     * 查询字段，默认为所有
     */
    protected String field = DEFAULT_FIELDS;

    /**
     * 是否消除重复元素
     */
    protected boolean distinct = false;

    /**
     * 条件查询
     */
    protected final Where where = new Where();

    /**
     * 分页查询
     */
    protected Pager pager;

    public Query() {
        this(DEFAULT_FIELDS);
    }

    public Query(String field) {
        this.field = field;
    }

    public static Query builder() {
        return builder(DEFAULT_FIELDS);
    }

    public static Query builder(String field) {
        return new Query(field);
    }

    public String field() {
        return field;
    }

    public void field(String field) {
        this.field = field;
    }

    public boolean distinct() {
        return distinct;
    }

    public void distinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * 不加条件的SELECT单条数据查询
     */
    public Query select() {
        return select("*");
    }

    /**
     * 不加条件的SELECT单条数据查询
     *
     * @param field 要查询的字段，示例："id, name, age"
     */
    public Query select(String field) {
        this.field = field;
        return this;
    }

    public Where where() {
        return where;
    }

    /**
     * Where与查询
     *
     * @param key       查询字段
     * @param operation 查询操作，可以为=、>=、<=等操作
     * @param value     查询的值
     */
    public Where where(String key, String operation, Object value) {
        return where.and(key, operation, value);
    }

    /**
     * Where与查询
     *
     * @param key       查询字段
     * @param operation 查询操作，可以为=、>=、<=等操作
     * @param value     查询的值
     * @param codec     查询值对应的ProtoBuf编码解码器，服务于存储字段为二进制
     */
    public Where where(String key, String operation, Object value, ProtoField<?> codec) {
        return where.and(key, operation, value, codec);
    }

    /**
     * Where与查询
     *
     * @param key       查询字段
     * @param operation 查询操作，可以为=、>=、<=等操作
     * @param value     查询的值
     * @param schema    查询字段值所在层级的元数据，服务于存储字段为二进制，注意查询字段与Schema必须要在同一层级避免Key混淆
     */
    public Where where(String key, String operation, Object value, ProtoSchema schema) {
        ProtoField<?> codec = schema.getField(key);
        return where.and(key, operation, value, codec);
    }

    public Pager pager() {
        return pager;
    }

    public void pager(Pager pager) {
        this.pager = pager;
    }

    /**
     * 分页查询
     *
     * @param start 分页开始位置
     * @param limit 分页条数
     */
    public Query limit(int start, int limit) {
        if (pager == null) {
            pager = new Pager(start, limit);
        } else {
            pager.setStart(start);
            pager.setLimit(limit);
        }
        return this;
    }
}
