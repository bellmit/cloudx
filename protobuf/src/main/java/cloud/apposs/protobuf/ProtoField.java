package cloud.apposs.protobuf;

import java.io.Serializable;

import cloud.apposs.protobuf.ProtoFieldFactory.ProtoFieldCodec;

/**
 * 协议字段包装
 */
public class ProtoField<T> implements Serializable {
	private static final long serialVersionUID = 6448343484206757206L;
	
	/** 字段序号，即Protocol Buffer的Key */
	private final int key;
	
	/** 字段，对于Class对象是Fileld类，对于Map是Key值，允许为空 */
	private final Object field;
	
	/** 字段类型，序列化与反序列化类型时需要，不允许为空 */
	private final ProtoFieldCodec<T> codec;
	
	/** 字段元信息数据，序列化与反序列化类型时需要，不允许为空 */
	private final ProtoSchema schema;
	
	public ProtoField(int number, Object field, Class<T> clazz) {
		this(number, field, ProtoFieldFactory.getFieldCodec(clazz), null);
	}

	public ProtoField(int key, Object field, ProtoFieldCodec<T> codec) {
		this(key, field, codec, null);
	}
	
	public ProtoField(int key, Object field,
			ProtoFieldCodec<T> codec, ProtoSchema schema) {
		this.key = key;
		this.field = field;
		this.codec = codec;
		this.schema = schema;
	}

	public int getKey() {
		return key;
	}

	public Object getField() {
		return field;
	}

	public ProtoFieldCodec<T> getCodec() {
		return codec;
	}

	public ProtoSchema getSchema() {
		return schema;
	}
}
