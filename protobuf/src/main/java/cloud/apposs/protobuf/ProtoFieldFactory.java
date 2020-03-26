package cloud.apposs.protobuf;

import cloud.apposs.util.Param;
import cloud.apposs.util.Table;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协议字段编码解码
 */
public class ProtoFieldFactory {
	public static final ProtoFieldInt32 INT32 = new ProtoFieldInt32();
	public static final ProtoFieldInt64 INT64 = new ProtoFieldInt64();
	public static final ProtoFieldByte BYTE = new ProtoFieldByte();
	public static final ProtoFieldBoolean BOOLEAN = new ProtoFieldBoolean();
	public static final ProtoFieldShort SHORT = new ProtoFieldShort();
	public static final ProtoFieldFloat FLOAT = new ProtoFieldFloat();
	public static final ProtoFieldDouble DOUBLE = new ProtoFieldDouble();
	public static final ProtoFieldString STRING = new ProtoFieldString();
	public static final ProtoFieldCalendar CALENDAR = new ProtoFieldCalendar();
	public static final ProtoFieldBuffer BUFFER = new ProtoFieldBuffer();
	public static final ProtoFieldMap MAP = new ProtoFieldMap();
	public static final ProtoFieldList LIST = new ProtoFieldList();
	public static final ProtoFieldParam PARAM = new ProtoFieldParam();
	public static final ProtoFieldTable TABLE = new ProtoFieldTable();
	public static final ProtoFieldObject OBJECT = new ProtoFieldObject();
	public static final Map<Class<?>, ProtoFieldCodec<?>> fieldValues = 
		new HashMap<Class<?>, ProtoFieldCodec<?>>();
	static {
		fieldValues.put(int.class, INT32);
		fieldValues.put(Integer.class, INT32);
		fieldValues.put(long.class, INT64);
		fieldValues.put(Long.class, INT64);
		fieldValues.put(byte.class, BYTE);
		fieldValues.put(Byte.class, BYTE);
		fieldValues.put(boolean.class, BOOLEAN);
		fieldValues.put(Boolean.class, BOOLEAN);
		fieldValues.put(short.class, SHORT);
		fieldValues.put(Short.class, SHORT);
		fieldValues.put(float.class, FLOAT);
		fieldValues.put(Float.class, FLOAT);
		fieldValues.put(double.class, DOUBLE);
		fieldValues.put(Double.class, DOUBLE);
		fieldValues.put(String.class, STRING);
		fieldValues.put(Calendar.class, CALENDAR);
		fieldValues.put(ByteBuffer.class, BUFFER);
		fieldValues.put(Map.class, MAP);
		fieldValues.put(List.class, LIST);
		fieldValues.put(Param.class, PARAM);
		fieldValues.put(Table.class, TABLE);
	}
	public static final Map<Class<?>, Boolean> simpleFields = new HashMap<Class<?>, Boolean>();
	static {
		simpleFields.put(int.class, true);
		simpleFields.put(Integer.class, true);
		simpleFields.put(long.class, true);
		simpleFields.put(Long.class, true);
		simpleFields.put(byte.class, true);
		simpleFields.put(Byte.class, true);
		simpleFields.put(boolean.class, true);
		simpleFields.put(Boolean.class, true);
		simpleFields.put(short.class, true);
		simpleFields.put(Short.class, true);
		simpleFields.put(float.class, true);
		simpleFields.put(Float.class, true);
		simpleFields.put(double.class, true);
		simpleFields.put(Double.class, true);
		simpleFields.put(String.class, true);
		simpleFields.put(Calendar.class, true);
		simpleFields.put(ByteBuffer.class, true);
	}
	
	/**
	 * 获取指定字段类型编码解码器
	 */
	@SuppressWarnings("unchecked")
	public static <T> ProtoFieldCodec<T> getFieldCodec(Class<T> clazz) {
		ProtoFieldCodec codec = (ProtoFieldCodec) fieldValues.get(clazz);
		if (codec == null) {
			codec = ProtoFieldFactory.OBJECT;
		}
		return codec; 
	}
	
	/**
	 * 添加自定义字段类型编码解码器
	 */
	public static void addFieldCodec(Class<?> type, ProtoFieldCodec<?> field) {
		fieldValues.put(type, field);
	}
	
	/**
	 * 判断字段类型是否为简单类型
	 */
	public static boolean isSimpleField(Class<?> type) {
		return simpleFields.containsKey(type);
	}

	/**
	 * 正数/负数字节码补码压缩，
	 * 在本协议中只针对负数压缩，负数因为字节大都是1，所以需要通过ZIGZAG算法进行压缩，
	 * 压缩算法细节详见：
	 * https://blog.csdn.net/zgwangbo/article/details/51590186
	 * 
	 * @param  value 压缩前的数字
	 * @return 压缩后的数字
	 */
	public static int int32ToZigzag(int value) {
		return (value << 1) ^ (value >> 31);
	}
	
	/**
	 * 正数/负数字节码补码解压，
	 * 在本协议中只针对负数压缩，负数因为字节大都是1，所以需要通过ZIGZAG算法进行压缩，
	 * 压缩算法细节详见：
	 * https://blog.csdn.net/zgwangbo/article/details/51590186
	 * 
	 * @param  value 解压前的数字
	 * @return 解缩后的数字
	 */
	public static int zigzagToInt32(int value) {
		return (value >>> 1) ^ -(value & 1);
	}
	
	public static long int64ToZigzag(long value) {
		return (value << 1) ^ (value >> 63);
	}
	
	public static long zigzagToInt64(long value) {
		return (value >>> 1) ^ -(value & 1);
	}
	
	/**
	 * 数据字段协议编码与解码操作
	 */
	public static interface ProtoFieldCodec<T> {
		/**
		 * 解码数据
		 * 
		 * @param  buffer 字节码
		 * @return 解码数据
		 * @throws ProtoBufException
		 */
		T readVarValue(ProtoBuf buffer) throws ProtoBufException;
		
		/**
		 * 解码数据
		 * 
		 * @param  buffer 字节码
		 * @param  schema 元信息数据
		 * @return 解码数据
		 * @throws ProtoBufException
		 */
		T readVarValue(ProtoBuf buffer, ProtoSchema schema) throws ProtoBufException;
		
		/**
		 * 解码数据
		 * 
		 * @param  buffer 字节码
		 * @param  type   数据类型
		 * @param  schema 元信息数据
		 * @return 解码数据
		 * @throws ProtoBufException
		 */
		T readVarValue(ProtoBuf buffer, byte type, ProtoSchema schema) throws ProtoBufException;
		
		/**
		 * 编码数据
		 * 
		 * @param buffer 字节码
		 * @param key    数据存储Key
		 * @param value  原始数据
		 */
		void writeVarValue(ProtoBuf buffer, int key, T value) throws ProtoBufException;
		
		/**
		 * 编码数据
		 * 
		 * @param buffer 字节码
		 * @param key    数据存储Key
		 * @param value  原始数据
		 * @param schema 元信息数据
		 */
		void writeVarValue(ProtoBuf buffer, int key, T value,
                           ProtoSchema schema) throws ProtoBufException;
	}
	
	public static abstract class AbstractProtoFieldCodec<T> implements ProtoFieldCodec<T> {
		@Override
		public T readVarValue(ProtoBuf buffer) throws ProtoBufException {
			ProtoKey key = buffer.doDecodeKey();
			return readVarValue(buffer, key.getType(), null);
		}

		@Override
		public T readVarValue(ProtoBuf buffer, ProtoSchema schema) throws ProtoBufException {
			ProtoKey key = buffer.doDecodeKey();
			return readVarValue(buffer, key.getType(), schema);
		}
		
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, T value) {
			writeVarValue(buffer, key, value, null);
		}
	}
	
	public static final class ProtoFieldInt32 extends AbstractProtoFieldCodec<Integer> {
		@Override
		public Integer readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				return zigzagToInt32(buffer.doReadVarInt32());
			} else {
				return buffer.doReadVarInt32();
			}
		}
		
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Integer value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt32(int32ToZigzag(value));
			} else {
				buffer.doWriteVarInt32(value);
			}
		}

		@Override
		public String toString() {
			return "CODEC_INT32";
		}
	};
	
	public static final class ProtoFieldInt64 extends AbstractProtoFieldCodec<Long> {
		@Override
		public Long readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT64) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				return zigzagToInt64(buffer.doReadVarInt64());
			} else {
				return buffer.doReadVarInt64();
			}
		}
		
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Long value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT64);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt64(int64ToZigzag(value));
			} else {
				buffer.doWriteVarInt64(value);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_INT64";
		}
	};
	
	public static final class ProtoFieldByte extends AbstractProtoFieldCodec<Byte> {
		@Override
		public Byte readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				return (byte) zigzagToInt32(buffer.doReadVarInt32());
			} else {
				return (byte) buffer.doReadVarInt32();
			}
		}

		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Byte value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt32(int32ToZigzag(value));
			} else {
				buffer.doWriteVarInt32(value);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_BYTE";
		}
	};
	
	public static final class ProtoFieldBoolean extends AbstractProtoFieldCodec<Boolean> {
		@Override
		public Boolean readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			byte value = 0;
			if (buffer.isZigzag()) {
				value = (byte) zigzagToInt32(buffer.doReadVarInt32());
			} else {
				value = (byte) buffer.doReadVarInt32();
			}
			return value == 0 ? false : true;
		}

		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Boolean value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt32(int32ToZigzag(value ? 1 : 0));
			} else {
				buffer.doWriteVarInt32(value ? 1 : 0);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_BOOLEAN";
		}
	};
	
	public static final class ProtoFieldShort extends AbstractProtoFieldCodec<Short> {
		@Override
		public Short readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				return (short) zigzagToInt32(buffer.doReadVarInt32());
			} else {
				return (short) buffer.doReadVarInt32();
			}
		}

		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Short value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt32(int32ToZigzag(value));
			} else {
				buffer.doWriteVarInt32(value);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_SHORT";
		}
	};
	
	public static final class ProtoFieldFloat extends AbstractProtoFieldCodec<Float> {
		@Override
		public Float readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				int value = zigzagToInt32(buffer.doReadVarInt32());
				return Float.intBitsToFloat(value);
			} else {
				int value = buffer.doReadVarInt32();
				return Float.intBitsToFloat(value);
			}
		}

		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Float value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				int rawValue = int32ToZigzag(Float.floatToRawIntBits(value));
				buffer.doWriteVarInt32(rawValue);
			} else {
				int rawValue = Float.floatToRawIntBits(value);
				buffer.doWriteVarInt32(rawValue);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_FLOAT";
		}
	};
	
	public static final class ProtoFieldDouble extends AbstractProtoFieldCodec<Double> {
		@Override
		public Double readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT) {
				throw ProtoBufException.malformedVarInt();
			}
			
			if (buffer.isZigzag()) {
				long value = zigzagToInt64(buffer.doReadVarInt64());
				return Double.longBitsToDouble(value);
			} else {
				long value = buffer.doReadVarInt64();
				return Double.longBitsToDouble(value);
			}
		}

		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Double value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT);
			if (buffer.isZigzag()) {
				long rawValue = int64ToZigzag(Double.doubleToRawLongBits(value));
				buffer.doWriteVarInt64(rawValue);
			} else {
				long rawValue = Double.doubleToRawLongBits(value);
				buffer.doWriteVarInt64(rawValue);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_DOUBLE";
		}
	};
	
	public static final class ProtoFieldString extends AbstractProtoFieldCodec<String> {
		@Override
		public String readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.LEN_DELIMI) {
				throw ProtoBufException.malformedVarInt();
			}
			
			// 解码字符串长度
			int length = buffer.doReadVarInt32();
			if (length < 0) {
				throw ProtoBufException.negativeSize();
			}
			
			int index = buffer.readIdx();
			try {
				byte[] datas = ProtoBuf.toByteArray(buffer.buffer(), index, length);
				return new String(datas, 0, length, buffer.charset());
			} finally {
				buffer.readIdx(index + length);
			}
		}
		
		/**
		 * 字符串存储形式Key(VarInt)->Length(VarInt)->value(String Bytes)，
		 * 注意字符串相对要多存储一个Length字段用于标识字符串长度，所以字节反而增加没有压缩，
		 * 其内存表现形式如下：
		 * <pre>
		 * +----------+-------------+
		 * |  Length  | String Byte |
		 * +----------+-------------+
		 * | 00001010 | 01001011... |
		 * +----------+-------------+
		 * </pre>
		 */
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, String value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.LEN_DELIMI);
			byte[] valueBytes = value.getBytes(buffer.charset());
			if (valueBytes == null) {
				// 无效字符存储为0
				buffer.doWriteVarInt32(0);
			} else {
				int length = valueBytes.length;
				buffer.writeIdx(buffer.writeIdx() + length);;
				// 开始字节存储字符串长度
				buffer.doWriteVarInt32(length);
				// 往后字节位存储字符串编码
				buffer.doAutoExpand(length);
				buffer.buffer().put(valueBytes, 0, length);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_STRING";
		}
	};
	
	public static final class ProtoFieldCalendar extends AbstractProtoFieldCodec<Calendar> {
		@Override
		public Calendar readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.VARINT64) {
				throw ProtoBufException.malformedVarInt();
			}
			
			Calendar value = Calendar.getInstance();
			if (buffer.isZigzag()) {
				value.setTimeInMillis(zigzagToInt64(buffer.doReadVarInt64()));
			} else {
				value.setTimeInMillis(buffer.doReadVarInt64());
			}
			return value;
		}
		
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, Calendar value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.VARINT64);
			if (buffer.isZigzag()) {
				buffer.doWriteVarInt64(int64ToZigzag(value.getTimeInMillis()));
			} else {
				buffer.doWriteVarInt64(value.getTimeInMillis());
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_CALENDAR";
		}
	};
	
	public static final class ProtoFieldBuffer extends AbstractProtoFieldCodec<ByteBuffer> {
		@Override
		public ByteBuffer readVarValue(ProtoBuf buffer, byte type, 
				ProtoSchema schema) throws ProtoBufException {
			if (type != ProtoType.LEN_DELIMI) {
				throw ProtoBufException.malformedVarInt();
			}
			
			// 解码字符串长度
			int length = buffer.doReadVarInt32();
			if (length < 0) {
				throw ProtoBufException.negativeSize();
			}
			
			int index = buffer.readIdx();
			try {
				byte[] datas = ProtoBuf.toByteArray(buffer.buffer(), index, length);
				return ByteBuffer.wrap(datas);
			} finally {
				buffer.readIdx(index + length);
			}
		}
		
		/**
		 * 字节数组存储形式Key(VarInt)->Length(VarInt)->value(Buffer Bytes)，
		 * 注意字符串相对要多存储一个Length字段用于标识字符串长度，所以字节反而增加没有压缩，
		 * 其内存表现形式如下：
		 * <pre>
		 * +----------+-------------+
		 * |  Length  | Buffer Byte |
		 * +----------+-------------+
		 * | 00001010 | 01001011... |
		 * +----------+-------------+
		 * </pre>
		 */
		@Override
		public void writeVarValue(ProtoBuf buffer, int key, ByteBuffer value, ProtoSchema schema) {
			buffer.doEncodeKey(key, ProtoType.LEN_DELIMI);
			byte[] valueBytes = ProtoBuf.toByteArray(value, value.position(), value.limit());
			if (valueBytes == null) {
				// 无效字符存储为0
				buffer.doWriteVarInt32(0);
			} else {
				int length = valueBytes.length;
				buffer.writeIdx(buffer.writeIdx() + length);;
				// 先存储字节长度
				buffer.doWriteVarInt32(length);
				// 往后存储字节数组
				buffer.doAutoExpand(length);
				buffer.buffer().put(valueBytes, 0, length);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_BUFFER";
		}
	};
	
	public static final class ProtoFieldMap extends AbstractProtoFieldCodec<Map<Object, Object>> {
		@Override
		public Map<Object, Object> readVarValue(ProtoBuf buffer, byte type,
				ProtoSchema schema) throws ProtoBufException {
			// 先检查对象是否有开始边界
			if (type != ProtoType.GROUP_BEG) {
				throw ProtoBufException.malformedVarInt();
			}
			
			Map<Object, Object> instance = new HashMap<Object, Object>();
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			while(true) {
				// 不断解析字节数据，直到到达对象边界时解码结束
				ProtoKey protoKey = buffer.doDecodeKey();
				if (protoKey.getType() == ProtoType.GROUP_END) {
					break;
				}
				ProtoField<?> protoField = protoFieldList.get(protoKey.getKey());
				Object mapKey = protoField.getField();
				ProtoFieldCodec<?> codec = protoField.getCodec();
				ProtoSchema fieldSchema = protoField.getSchema();
				Object mapValue = codec.readVarValue(buffer, protoKey.getType(), fieldSchema);
				instance.put(mapKey, mapValue);
			}
			return instance;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void writeVarValue(ProtoBuf buffer, int key, Map<Object, Object> value, ProtoSchema schema) {
			// 编码对象时需要定义Map的字节边界，以便于当Map又包含Map时能够通过边界解码
			buffer.doEncodeKey(key, ProtoType.GROUP_BEG);
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			for (int i = 0; i < protoFieldList.size(); i++) {
				ProtoField<?> protoField = protoFieldList.get(i);
				int fieldKey = protoField.getKey();
				Object mapKey = protoField.getField();
				Object mapValue = value.get(mapKey);
				if (mapValue == null) {
					continue;
				}
				ProtoSchema fieldSchema = protoField.getSchema();
				ProtoFieldCodec codec = protoField.getCodec();
				try {
					codec.writeVarValue(buffer, fieldKey, mapValue, fieldSchema);
				} catch(ClassCastException e){
					// Schema可能拼错，输出对应拼错的Schema字段
					throw new ClassCastException("Field['" + mapKey + "'] Cast Error:" + e.getMessage());
				}
			}
			buffer.doEncodeKey(key, ProtoType.GROUP_END);
		}
		
		@Override
		public String toString() {
			return "CODEC_MAP";
		}
	};
	
	public static final class ProtoFieldList extends AbstractProtoFieldCodec<List<Object>> {
		@Override
		@SuppressWarnings("unchecked")
		public List<Object> readVarValue(ProtoBuf buffer, byte type, ProtoSchema schema) throws ProtoBufException {
			// 先检查对象是否有开始边界
			if (type != ProtoType.GROUP_BEG) {
				throw ProtoBufException.malformedVarInt();
			}
			
			List<Object> instance = new ArrayList<Object>();
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			ProtoField<?> protoField = protoFieldList.get(0);
			ProtoFieldCodec codec = protoField.getCodec();
			ProtoSchema fieldSchema = protoField.getSchema();
			while (true) {
				// 不断解析字节数据，直到到达对象边界时解码结束
				ProtoKey protoKey = buffer.doDecodeKey();
				byte protoType = protoKey.getType();
				if (protoType == ProtoType.GROUP_END) {
					break;
				}
				Object value = codec.readVarValue(buffer, protoType, fieldSchema);
				instance.add(value);
			}
			
			return instance;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void writeVarValue(ProtoBuf buffer, int key, List<Object> value, ProtoSchema schema) {
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			ProtoField<?> protoField = protoFieldList.get(0);
			ProtoFieldCodec codec = protoField.getCodec();
			ProtoSchema fieldSchema = protoField.getSchema();
			// 编码对象时需要定义List的字节边界，以便于当List又包含List时能够通过边界解码
			buffer.doEncodeKey(key, ProtoType.GROUP_BEG);
			for (Object instance : value) {
				codec.writeVarValue(buffer, key, instance, fieldSchema);
			}
			buffer.doEncodeKey(key, ProtoType.GROUP_END);
		}

		@Override
		public String toString() {
			return "CODEC_LIST";
		}
	};
	
	public static final class ProtoFieldParam extends AbstractProtoFieldCodec<Param> {
		@Override
		public Param readVarValue(ProtoBuf buffer, byte type,
				ProtoSchema schema) throws ProtoBufException {
			// 先检查对象是否有开始边界
			if (type != ProtoType.GROUP_BEG) {
				throw ProtoBufException.malformedVarInt();
			}
			
			Param instance = new Param();
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			while(true) {
				// 不断解析字节数据，直到到达对象边界时解码结束
				ProtoKey protoKey = buffer.doDecodeKey();
				if (protoKey.getType() == ProtoType.GROUP_END) {
					break;
				}
				ProtoField<?> protoField = protoFieldList.get(protoKey.getKey());
				String mapKey = (String) protoField.getField();
				ProtoFieldCodec<?> codec = protoField.getCodec();
				ProtoSchema fieldSchema = protoField.getSchema();
				Object mapValue = codec.readVarValue(buffer, protoKey.getType(), fieldSchema);
				instance.setObject(mapKey, mapValue);
			}
			return instance;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void writeVarValue(ProtoBuf buffer, int key, Param value, ProtoSchema schema) {
			// 编码对象时需要定义Map的字节边界，以便于当Map又包含Map时能够通过边界解码
			buffer.doEncodeKey(key, ProtoType.GROUP_BEG);
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			for (int i = 0; i < protoFieldList.size(); i++) {
				ProtoField<?> protoField = protoFieldList.get(i);
				int fieldKey = protoField.getKey();
				Object mapKey = protoField.getField();
				Object mapValue = value.get(mapKey);
				if (mapValue == null) {
					continue;
				}
				ProtoSchema fieldSchema = protoField.getSchema();
				ProtoFieldCodec codec = protoField.getCodec();
				try {
					codec.writeVarValue(buffer, fieldKey, mapValue, fieldSchema);
				} catch(ClassCastException e){
					// Schema可能拼错，输出对应拼错的Schema字段
					throw new ClassCastException("Field['" + mapKey + "'] Cast Error:" + e.getMessage());
				}
			}
			buffer.doEncodeKey(key, ProtoType.GROUP_END);
		}
		
		@Override
		public String toString() {
			return "CODEC_PARAM";
		}
	};
	
	public static final class ProtoFieldTable extends AbstractProtoFieldCodec<Table<Object>> {
		@Override
		@SuppressWarnings("unchecked")
		public Table<Object> readVarValue(ProtoBuf buffer, byte type, ProtoSchema schema) throws ProtoBufException {
			// 先检查对象是否有开始边界
			if (type != ProtoType.GROUP_BEG) {
				throw ProtoBufException.malformedVarInt();
			}
			
			Table<Object> instance = new Table<Object>();
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			ProtoField<?> protoField = protoFieldList.get(0);
			ProtoFieldCodec codec = protoField.getCodec();
			ProtoSchema fieldSchema = protoField.getSchema();
			while (true) {
				// 不断解析字节数据，直到到达对象边界时解码结束
				ProtoKey protoKey = buffer.doDecodeKey();
				byte protoType = protoKey.getType();
				if (protoType == ProtoType.GROUP_END) {
					break;
				}
				Object value = codec.readVarValue(buffer, protoType, fieldSchema);
				instance.add(value);
			}
			
			return instance;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void writeVarValue(ProtoBuf buffer, int key, Table<Object> value, ProtoSchema schema) {
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			ProtoField<?> protoField = protoFieldList.get(0);
			ProtoFieldCodec codec = protoField.getCodec();
			ProtoSchema fieldSchema = protoField.getSchema();
			// 编码对象时需要定义List的字节边界，以便于当List又包含List时能够通过边界解码
			buffer.doEncodeKey(key, ProtoType.GROUP_BEG);
			for (Object instance : value) {
				try {
					codec.writeVarValue(buffer, key, instance, fieldSchema);
				} catch(ClassCastException e){
					// Schema可能拼错，输出对应拼错的Schema字段
					throw new ClassCastException("Field['" + protoField.getField() + "'] Cast Error:" + e.getMessage());
				}
			}
			buffer.doEncodeKey(key, ProtoType.GROUP_END);
		}

		@Override
		public String toString() {
			return "CODEC_TABLE";
		}
	};
	
	public static final class ProtoFieldObject extends AbstractProtoFieldCodec<Object> {
		@Override
		public Object readVarValue(ProtoBuf buffer, byte type, ProtoSchema schema) throws ProtoBufException {
			try {
				// 先检查对象是否有开始边界
				if (type != ProtoType.GROUP_BEG) {
					throw ProtoBufException.malformedVarInt();
				}
				
				Class<?> typeClass = schema.getFiledType();
				Object instance = typeClass.newInstance();
				while(true) {
					// 不断解析字节数据，直到到达对象边界时解码结束
					ProtoKey protoKey = buffer.doDecodeKey();
					byte protoType = protoKey.getType();
					if (protoType == ProtoType.GROUP_END) {
						break;
					}
					ProtoField<?> protoField = schema.getField(protoKey.getKey());
					ProtoFieldCodec<?> codec = protoField.getCodec();
					Object value = codec.readVarValue(buffer, protoType, protoField.getSchema());
					Field field = (Field) protoField.getField();
					field.setAccessible(true);
					field.set(instance, value);
				}
				return instance;
			} catch(InstantiationException e) {
				throw ProtoBufException.reflectInstantiation(schema.getFiledType(), e);
			} catch(IllegalAccessException e) {
				throw ProtoBufException.reflectInstantiation(schema.getFiledType(), e);
			} catch(SecurityException e) {
				throw ProtoBufException.reflectCall(e);
			}
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public void writeVarValue(ProtoBuf buffer, int key, Object value, ProtoSchema schema) {
			try {
				// 编码对象时需要定义对象的字节边界，以便于当对象又包含对象时能够通过边界解码
				buffer.doEncodeKey(key, ProtoType.GROUP_BEG);
				for (ProtoField<?> protoField : schema.getFieldList()) {
					ProtoFieldCodec codec = protoField.getCodec();
					Field field = (Field) protoField.getField();
					int fieldKey = protoField.getKey();
					ProtoSchema fieldSchema = protoField.getSchema();
					field.setAccessible(true);
					Object fieldValue = field.get(value);
					if (fieldValue == null) {
						continue;
					}
					try {
						codec.writeVarValue(buffer, fieldKey, fieldValue, fieldSchema);
					} catch(ClassCastException e){
						// Schema可能拼错，输出对应拼错的Schema字段
						throw new ClassCastException("Field['" + field + "'] Cast Error:" + e.getMessage());
					}
				}
				buffer.doEncodeKey(key, ProtoType.GROUP_END);
			} catch (SecurityException e) {
				throw ProtoBufException.reflectCall(e);
			} catch (IllegalArgumentException e) {
				throw ProtoBufException.reflectCall(e);
			} catch (IllegalAccessException e) {
				throw ProtoBufException.reflectCall(e);
			}
		}
		
		@Override
		public String toString() {
			return "CODEC_OBJECT";
		}
	};
}
