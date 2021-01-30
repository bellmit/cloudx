package cloud.apposs.cachex.storage;

import java.util.LinkedList;
import java.util.List;

import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.StrUtil;

/**
 * 表元信息，主要服务于表格创建
 */
public final class Metadata {
    public static final int COLUMN_TYPE_INT = 0;
    public static final int COLUMN_TYPE_STRING = 1;
    public static final int COLUMN_TYPE_LONG = 2;
    public static final int COLUMN_TYPE_DATE = 3;
    public static final int COLUMN_TYPE_BINARY = 4;

    /**
     * 表名
     */
    private final String table;

    /**
     * 数据表编码
     */
    private final String charset;

    /**
     * 字段列表
     */
    private final List<Column> columnList = new LinkedList<Column>();

    /**
     * 索引列表
     */
    private final List<Index> indexList = new LinkedList<Index>();

    public Metadata(String table) {
        this(table, CharsetUtil.UTF_8.name());
    }

    public Metadata(String table, String charset) {
        if (StrUtil.isEmpty(table)) {
            throw new IllegalArgumentException("table");
        }
        this.table = table;
        this.charset = charset;
    }

    public String getTable() {
        return table;
    }

    public String getCharset() {
        return charset;
    }

    public List<Column> getColumnList() {
        return columnList;
    }

    public List<Index> getIndexList() {
        return indexList;
    }

    /**
     * 添加表格主键字段
     */
    public final void addPrimaryColumn(String name, int type) {
        addColumn(new Column(name, type, -1, true, false, null));
    }

    /**
     * 添加表格主键字段
     */
    public final void addPrimaryColumn(String name, int type, int length) {
        addColumn(new Column(name, type, length, true, false, null));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(String name, int type) {
        addColumn(new Column(name, type, -1, false, false, null));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(String name, int type, int length) {
        addColumn(new Column(name, type, length, false, false, null));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(String name, int type, int length, String defaultVal) {
        addColumn(new Column(name, type, length, false, true, defaultVal));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(String name, int type, int length, boolean notNull) {
        addColumn(new Column(name, type, length, false, notNull, null));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(String name, int type, int length, boolean notNull, String defaultVal) {
        addColumn(new Column(name, type, length, false, notNull, defaultVal));
    }

    /**
     * 添加表格字段
     *
     * @param name 字段名称
     * @param type 字段类型
     * @param length 字段长度，大于0则标明长度
     * @param primary 是否为主键，主键字段不允许为空且自增
     * @param notNull 是否允许为空
     * @param defaultVal 默认值
     */
    public final void addColumn(String name, int type, int length,
                                boolean primary, boolean notNull, String defaultVal) {
        addColumn(new Column(name, type, length, primary, notNull, defaultVal));
    }

    /**
     * 添加表格字段
     */
    public final void addColumn(Column column) {
        columnList.add(column);
    }

    /**
     * 添加表格唯一索引
     */
    public final void addPrimaryIndex(String index) {
        addPrimaryIndex(index, null);
    }

    public final void addPrimaryIndex(String index, String name) {
        if (StrUtil.isEmpty(index)) {
            throw new IllegalArgumentException();
        }
        addIndex(new Index(true, index, name));
    }

    /**
     * 添加表格索引
     *
     * @param index 索引字段
     */
    public final void addIndex(String index) {
        addIndex(index, null);
    }

    public final void addIndex(String index, String name) {
        if (StrUtil.isEmpty(index)) {
            throw new IllegalArgumentException();
        }
        addIndex(new Index(false, index, name));
    }

    /**
     * 添加表格索引
     *
     * @param index 索引字段，可为多个用逗号分开，如aid, app, id
     * @param primary 是否为唯一主键
     */
    public final void addIndex(String index, boolean primary) {
        addIndex(index, primary, null);
    }

    public final void addIndex(String index, boolean primary, String name) {
        if (StrUtil.isEmpty(index)) {
            throw new IllegalArgumentException();
        }
        addIndex(new Index(primary, index, name));
    }

    /**
     * 添加表格索引
     */
    public final void addIndex(Index index) {
        indexList.add(index);
    }

    /**
     * 字段数据
     */
    public static class Column {
        /**
         * 字段名称
         */
        private final String name;

        /**
         * 字段类型
         */
        private final int type;

        /**
         * 字段长度，大于0则标明长度
         */
        private final int length;

        /**
         * 是否为主键，主键字段不允许为空且自增
         */
        private final boolean primary;

        /**
         * 是否允许为空
         */
        private final boolean notNull;

        /**
         * 默认值
         */
        private final String defaultVal;

        public Column(String name, int type, int length) {
            this(name, type, length, false, false, null);
        }

        public Column(String name, int type, int length, boolean primary) {
            this(name, type, length, primary, false, null);
        }

        public Column(String name, int type, int length, boolean notNull, String defaultVal) {
            this(name, type, length, false, notNull, defaultVal);
        }

        public Column(String name, int type, int length, boolean primary, boolean notNull, String defaultVal) {
            this.name = name;
            this.type = type;
            this.length = length;
            this.primary = primary;
            if (primary) {
                this.notNull = true;
            } else {
                this.notNull = notNull;
            }
            this.defaultVal = defaultVal;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public int getLength() {
            return length;
        }

        public boolean isNotNull() {
            return notNull;
        }

        public String getDefaultVal() {
            return defaultVal;
        }

        public boolean isPrimary() {
            return primary;
        }
    }

    /**
     * 字段索引
     */
    public static class Index {
        /**
         * 是否为主键索引，
         * 使用唯一索引不仅是为了性能，同时也为了数据的完整性，唯一索引不允许任何重复的值插入到表中
         */
        private final boolean primary;

        /**
         * 索引字段
         */
        private final String index;

        /**
         * 索引名称，默认为index_[索引字段]来命名
         */
        private final String name;

        public Index(boolean primary, String index) {
            this(primary, index, null);
        }

        public Index(boolean primary, String index, String name) {
            if (StrUtil.isEmpty(index)) {
                throw new IllegalArgumentException();
            }
            this.primary = primary;
            this.index = index;
            if (StrUtil.isEmpty(name)) {
                this.name = "index_" + index.replaceAll(",\\s+", "_");
            } else {
                this.name = name;
            }
        }

        public boolean isPrimary() {
            return primary;
        }

        public String getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }
}
