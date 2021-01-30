package cloud.apposs.protobuf;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cloud.apposs.util.Param;
import cloud.apposs.util.SysUtil;
import cloud.apposs.util.Table;
import cloud.apposs.protobuf.ProtoFieldFactory.ProtoFieldCodec;

/**
 * 对象序列化元信息数据，ProtoBuf会依据此元数据对对象进行序列化和反序列化操作，
 * 为了性能考虑，对象序列化和把序列化时此对象要是单例形式
 */
public class ProtoSchema implements Serializable {
    private static final long serialVersionUID = -1522954549554970989L;

    /**
     * 对象元数据缓存，主要为了提升性能，避免重复生成同一对象的元信息数据
     */
    private static final Map<Class<?>, ProtoSchema> schemas = new ConcurrentHashMap<Class<?>, ProtoSchema>();

    private final Class<?> filedType;

    private final List<ProtoField<?>> fieldList = new CopyOnWriteArrayList<ProtoField<?>>();
    /**
     * 对象元数据字段缓存，主要为了提升，通过字段名称来获取对应的字段编码/解码器
     */
    private final Map<Object, ProtoField<?>> fieldMap = new ConcurrentHashMap<Object, ProtoField<?>>();

    public ProtoSchema(Class<?> filedType) {
        this.filedType = filedType;
    }

    public Class<?> getFiledType() {
        return filedType;
    }

    public List<ProtoField<?>> getFieldList() {
        return fieldList;
    }

    public ProtoField<?> getField(int fieldNumber) {
        return fieldList.get(fieldNumber);
    }

    public ProtoField<?> getField(Object field) {
        return fieldMap.get(field);
    }

    /**
     * 获取Class对象元数据，Protocol Buffer需要通过元数据进行序列化与把序列化操作，
     * 注意序列化和反序列化的Schema必须一致
     *
     * @param clazz 对象Class类型
     * @return {@link ProtoSchema}
     */
    public static ProtoSchema getSchema(Class<?> clazz) {
        SysUtil.checkNotNull(clazz, "clazz");

        synchronized (ProtoSchema.class) {
            // 先从缓存判断是否已经存在对象对应的元数据，如果有直接返回
            ProtoSchema schema = schemas.get(clazz);
            if (schema != null) {
                return schema;
            }

            schema = new ProtoSchema(clazz);
            Field[] fields = clazz.getDeclaredFields();
            int index = 0;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                int mod = field.getModifiers();
                // 静态字段和transient字段不做序列化
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
                ProtoSchema fieldSchema = null;
                if (!ProtoFieldFactory.isSimpleField(fieldType)) {
                    // 属性字段值为复杂对象，递归解析直到最终字段为简单类型字段
                    fieldSchema = getSchema(fieldType);
                }
                schema.add(index++, field, codec, fieldSchema);
            }
            schemas.put(clazz, schema);
            return schema;
        }
    }

    /**
     * 获取Map元信息数据，根据Map不同的Value类型生成对应的解码器，
     * 因为Key值不变，所以Key值不会序列化到内存中，仅保存到Schema中以便于反序列化时解码Key值
     *
     * @param map Map数据
     * @return {@link ProtoSchema}
     */
    public static ProtoSchema getSchema(Map<?, ?> map) {
        SysUtil.checkNotNull(map, "map");

        ProtoSchema schema = new ProtoSchema(Map.class);
        int index = 1;
        for (Entry<?, ?> entry : map.entrySet()) {
            Object fieldKey = entry.getKey();
            Object fieldValue = entry.getValue();
            Class<?> fieldType = fieldValue.getClass();
            ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
            ProtoSchema fieldSchema = null;
            if (!ProtoFieldFactory.isSimpleField(fieldType)) {
                // 属性字段值为复杂对象，递归解析直到最终字段为简单类型字段
                fieldSchema = getSchema(fieldType);
            }
            schema.add(index++, fieldKey, codec, fieldSchema);
        }
        return schema;
    }

    /**
     * 获取List元信息数据
     *
     * @param list      List数据
     * @param fieldType List泛型类型
     * @return {@link ProtoSchema}
     */
    public static ProtoSchema getSchema(Class<List> list, Class<?> fieldType) {
        SysUtil.checkNotNull(list, "list");
        SysUtil.checkNotNull(fieldType, "fieldType");

        ProtoSchema schema = new ProtoSchema(fieldType);
        ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
        ProtoSchema fieldSchema = null;
        if (!ProtoFieldFactory.isSimpleField(fieldType)) {
            // 属性字段值为复杂对象，递归解析直到最终字段为简单类型字段
            fieldSchema = getSchema(fieldType);
        }
        schema.add(0, fieldType, codec, fieldSchema);
        return schema;
    }

    /**
     * 获取List元信息数据
     *
     * @param list        List数据
     * @param fieldType   List泛型类型
     * @param fieldSchema List值元数据信息，值为复杂对象
     * @return {@link ProtoSchema}
     */
    public static ProtoSchema getSchema(Class<List> list, Class<?> fieldType, ProtoSchema fieldSchema) {
        SysUtil.checkNotNull(list, "list");
        SysUtil.checkNotNull(fieldType, "fieldType");

        ProtoSchema schema = new ProtoSchema(fieldType);
        ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
        schema.add(0, fieldType, codec, fieldSchema);
        return schema;
    }

    /**
     * 创建对象元数据
     */
    public static ProtoSchema objectSchema(Class<?> clazz) {
        return new ProtoSchema(clazz);
    }

    /**
     * 创建Map类元数据
     */
    public static ProtoSchema mapSchema() {
        return new ProtoSchema(Map.class);
    }

    /**
     * 创建List类元数据
     */
    public static ProtoSchema listSchema() {
        return new ProtoSchema(List.class);
    }

    public static ProtoSchema listSchema(Class<?> fieldType) {
        return getSchema(List.class, fieldType);
    }

    public static ProtoSchema listSchema(Class<?> fieldType, ProtoSchema fieldSchema) {
        return getSchema(List.class, fieldType, fieldSchema);
    }

    /**
     * 创建Param类元数据
     */
    public static ProtoSchema paramSchema() {
        return new ProtoSchema(Param.class);
    }

    /**
     * 创建Table类元数据
     */
    public static ProtoSchema tableSchema() {
        return new ProtoSchema(Table.class);
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldKey  协议字段，即Map/Param的Key值
     * @param fieldType 要序列化/反序列化的字段类型
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addKey(String fieldKey, Class<T> fieldType) {
        int fieldNumber = fieldList.size();
        ProtoFieldCodec<T> codec = ProtoFieldFactory.getFieldCodec(fieldType);
        ProtoSchema schema = null;
        // 对象类为复杂对象
        if (!ProtoFieldFactory.isSimpleField(fieldType)) {
            schema = ProtoSchema.getSchema(fieldType);
        }
        return add(fieldNumber, fieldKey, codec, schema);
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldKey    协议字段，即Map/Param的Key值
     * @param fieldType   要序列化/反序列化的字段类型
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addKey(String fieldKey,
                                  Class<T> fieldType, ProtoSchema fieldSchema) {
        int fieldNumber = fieldList.size();
        ProtoFieldCodec<T> fieldCodec = ProtoFieldFactory.getFieldCodec(fieldType);
        return add(fieldNumber, fieldKey, fieldCodec, fieldSchema);
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldNumber 协议Key，Key值必须递增以便于做不同的Field匹配
     * @param fieldKey    协议字段，即Map/Param的Key值
     * @param fieldType   要序列化/反序列化的字段类型
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addKey(int fieldNumber,
                                  String fieldKey, Class<T> fieldType) {
        ProtoFieldCodec<T> fieldCodec = ProtoFieldFactory.getFieldCodec(fieldType);
        ProtoSchema fieldSchema = null;
        // 对象类为复杂对象
        if (!ProtoFieldFactory.isSimpleField(fieldType)) {
            fieldSchema = ProtoSchema.getSchema(fieldType);
        }
        return add(fieldNumber, fieldKey, fieldCodec, fieldSchema);
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldName Class对象属性名
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addField(String fieldName) {
        int fieldNumber = fieldList.size();
        try {
            Field field = filedType.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();
            ProtoFieldCodec<?> fieldCodec = ProtoFieldFactory.getFieldCodec(fieldType);
            ProtoSchema fieldSchema = null;
            // 对象类为复杂对象
            if (!ProtoFieldFactory.isSimpleField(fieldType)) {
                fieldSchema = ProtoSchema.getSchema(fieldType);
            }
            return add(fieldNumber, field, fieldCodec, fieldSchema);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return this;
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldName   Class对象属性名
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addField(String fieldName, ProtoSchema fieldSchema) {
        int fieldNumber = fieldList.size();
        try {
            Field field = filedType.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();
            ProtoFieldCodec<?> fieldCodec = ProtoFieldFactory.getFieldCodec(fieldType);
            return add(fieldNumber, field, fieldCodec, fieldSchema);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return this;
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldName   Class对象属性名
     * @param fieldType   字段类型
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addField(String fieldName,
                                    Class<?> fieldType, ProtoSchema fieldSchema) {
        int fieldNumber = fieldList.size();
        try {
            Field field = filedType.getDeclaredField(fieldName);
            ProtoFieldCodec<?> fieldCodec = ProtoFieldFactory.getFieldCodec(fieldType);
            return add(fieldNumber, field, fieldCodec, fieldSchema);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return this;
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldNumber 协议Key，Key值必须递增以便于做不同的Field匹配
     * @param fieldName   Class对象属性名
     * @param fieldType   字段类型
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addField(int fieldNumber,
                                    String fieldName, Class<?> fieldType, ProtoSchema fieldSchema) {
        try {
            Field field = filedType.getDeclaredField(fieldName);
            ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
            return add(fieldNumber, field, codec, fieldSchema);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return this;
    }

    /**
     * 添加字段元信息数据
     *
     * @param fieldNumber 协议Key，Key值必须递增以便于做不同的Field匹配
     * @param fieldName   Class对象属性名
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema addField(int fieldNumber,
                                    String fieldName, ProtoSchema fieldSchema) {
        try {
            Field field = filedType.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();
            ProtoFieldCodec<?> codec = ProtoFieldFactory.getFieldCodec(fieldType);
            return add(fieldNumber, field, codec, fieldSchema);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return this;
    }

    /**
     * 添加字段元信息数据，注意方法不是线程安全，外部调用最好在静态代码块初始化好
     *
     * @param fieldNumber 协议Key，Key值必须递归以便于做不同的Field匹配
     * @param field       协议字段，对于Class对象是Fileld类，对于Map是Key值
     * @param fieldCodec  协议字段对应编码解码器
     * @param fieldSchema 协议字段元数据，如果字段又是对象则用此参数需要递归解析
     * @return {@link ProtoSchema}
     */
    public <T> ProtoSchema add(int fieldNumber, Object field,
                               ProtoFieldCodec<T> fieldCodec, ProtoSchema fieldSchema) {
        // key number在每个字段中必须递增，以便于反序列化时通过key number来获取对应的字段
        if (fieldNumber < 0 || fieldNumber < fieldList.size()) {
            throw new IllegalArgumentException("number");
        }
        // 编码解码器不能为空
        if (fieldCodec == null) {
            throw new IllegalArgumentException("codec");
        }

        ProtoField<T> protoField = new ProtoField<T>(fieldNumber, field, fieldCodec, fieldSchema);
        fieldList.add(protoField);
        fieldMap.put(field, protoField);
        return this;
    }
}
