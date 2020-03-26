package cloud.apposs.protobuf;

import cloud.apposs.util.Param;
import cloud.apposs.util.SysUtil;
import cloud.apposs.util.Table;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Google Protocol序列化&反序列化协议封装实现，
 * 协议内部是采用类型Key->Value的二进制形式来存储数据，
 * 通过Key来读取对应的数据类型（不同的数据类型的长度不一样），
 * 消息经过序列化后会成为一个二进制数据流，其数据流在内存中的表示如下：
 * <pre>
 * +---Field1---+---Field2---+---Field3---+
 * | Key->Value | Key->Value | Key->Value |
 * +--------------------------------------+
 * +----------------Buffer----------------+
 * </pre>
 * 原理细节参考：
 * <pre>
 * https://www.ibm.com/developerworks/cn/linux/l-cn-gpb/index.html
 * https://blog.csdn.net/zgwangbo/article/details/51590186
 * https://developers.google.com/protocol-buffers/docs/encoding
 * </pre>
 */
public class ProtoBuf implements Serializable, Cloneable {
	private static final long serialVersionUID = 3083755223593767405L;
	
	/** 默认分配内存大小，单位字节 */
	public static final int DEFAULT_BUFFER_SIZE = 128;
	/** 默认的字符串字节编码 */
	public static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");
	/** 不指定Key序列化时的默认Key值 */
	public static final int DEFAULT_KEY = 1;
	
	/** INT最多一共要用5个字节存储 */
	public static final int MAX_INT32_SIZE = 5;
	/** LONG最多一共要用10个字节存储 */
	public static final int MAX_INT64_SIZE = 10;
	
	/** 编码位置索引 */
	private int writeIdx = 0;
	
	/** 解码位置索引 */
	private int readIdx = 0;
	
	/** 字节码 */
	private ByteBuffer buffer;
	
	/** 是否采用zigzag对负数进行压缩 */
	private final boolean zigzag;
	
	/** 字符串编码 */
	private Charset charset = DEFAULT_CHARSET;
	
	public ProtoBuf() {
		this(DEFAULT_BUFFER_SIZE, false, true);
	}
	
	public ProtoBuf(int size) {
		this(size, false, true);
	}
	
	public ProtoBuf(int size, boolean zigzag) {
		this(size, false, zigzag);
	}
	
	public ProtoBuf(boolean direct) {
		this(DEFAULT_BUFFER_SIZE, direct, true);
	}
	
	public ProtoBuf(boolean direct, boolean zigzag) {
		this(DEFAULT_BUFFER_SIZE, direct, zigzag);
	}
	
	public ProtoBuf(int size, boolean direct, boolean zigzag) {
		this.zigzag = zigzag;
		if (direct) {
			this.buffer = ByteBuffer.allocateDirect(size);
		} else {
			this.buffer = ByteBuffer.allocate(size);
		}
	}
	
	public ProtoBuf(ByteBuffer buffer) {
		this(buffer, true);
	}
	
	public ProtoBuf(ByteBuffer buffer, boolean zigzag) {
		this.zigzag = zigzag;
		this.buffer = buffer;
		this.readIdx = buffer.position();
		this.writeIdx = buffer.limit();
	}
	
	public static ProtoBuf wrap(ByteBuffer buffer) {
        return new ProtoBuf(buffer, true);
    }
	
	public static ProtoBuf wrap(byte[] buffer) {
        return wrap(buffer, 0, buffer.length);
    }
	
	public static ProtoBuf wrap(byte[] buffer, int offset, int length) {
		byte[] newBuffer = new byte[length];
		System.arraycopy(buffer, offset, newBuffer, 0, length);
        return new ProtoBuf(ByteBuffer.wrap(newBuffer), true);
    }
	
	public static ProtoBuf wrap(int value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT32_SIZE);
        return buffer.putInt(value);
    }
	
	public static ProtoBuf wrap(long value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT64_SIZE);
        return buffer.putLong(value);
    }
	
	public static ProtoBuf wrap(String value) {
		ProtoBuf buffer = new ProtoBuf(value.getBytes(DEFAULT_CHARSET).length + MAX_INT32_SIZE);
        return buffer.putString(value);
    }
	
	public static ProtoBuf wrap(float value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT32_SIZE);
        return buffer.putFloat(value);
    }
	
	public static ProtoBuf wrap(double value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT64_SIZE);
        return buffer.putDouble(value);
    }
	
	public static ProtoBuf wrap(short value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT32_SIZE);
        return buffer.putShort(value);
    }
	
	public static ProtoBuf wrap(byte value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT32_SIZE);
        return buffer.putByte(value);
    }
	
	public static ProtoBuf wrap(boolean value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT32_SIZE);
        return buffer.putBoolean(value);
    }
	
	public static ProtoBuf wrap(Calendar value) {
		ProtoBuf buffer = new ProtoBuf(MAX_INT64_SIZE);
        return buffer.putCalendar(value);
    }
	
	public static ProtoBuf wrap(Object value, ProtoSchema schema) {
		ProtoBuf buffer = new ProtoBuf(DEFAULT_BUFFER_SIZE);
        return buffer.putObject(value, schema);
    }
	
	public static ProtoBuf wrap(Param value, ProtoSchema schema) {
		ProtoBuf buffer = new ProtoBuf(DEFAULT_BUFFER_SIZE);
        return buffer.putParam(value, schema);
    }
	
	public static ProtoBuf wrap(Table<?> value, ProtoSchema schema) {
		ProtoBuf buffer = new ProtoBuf(DEFAULT_BUFFER_SIZE);
        return buffer.putTable(value, schema);
    }
	
	public static ProtoBuf wrap(Map<?, ?> value, ProtoSchema schema) {
		ProtoBuf buffer = new ProtoBuf(DEFAULT_BUFFER_SIZE);
        return buffer.putMap(value, schema);
    }
	
	public static ProtoBuf wrap(List<?> value, ProtoSchema schema) {
		ProtoBuf buffer = new ProtoBuf(DEFAULT_BUFFER_SIZE);
        return buffer.putList(value, schema);
    }
	
	public static ProtoBuf allocate() {
		return ProtoBuf.allocate(DEFAULT_BUFFER_SIZE);
	}
	
	public static ProtoBuf allocate(int size) {
        return ProtoBuf.allocate(size, false, true);
    }
	
	public static ProtoBuf allocate(int size, boolean zigzag) {
        return ProtoBuf.allocate(size, false, zigzag);
    }
	
	public static ProtoBuf allocateDirect() {
        return ProtoBuf.allocate(DEFAULT_BUFFER_SIZE, true, true);
    }
	
	public static ProtoBuf allocateDirect(int size) {
        return ProtoBuf.allocate(size, true, true);
    }
	
	public static ProtoBuf allocateDirect(int size, boolean zigzag) {
        return ProtoBuf.allocate(size, true, zigzag);
    }
	
	/**
	 * 创建指定内存大小的协议栈
	 * 
	 * @param  size   内存大小,单位字节
	 * @param  direct 是否用直接堆内存，
	 * 		 使用DirectBuffer可以提高网络IO的性能，减少JVM中Java内存和Native内存之间的转换损耗
	 * @return Google Protocol Buffer封装类
	 */
	public static ProtoBuf allocate(int size, boolean direct, boolean zigzag) {
        return new ProtoBuf(size, direct, zigzag);
    }
	
	/**
	 * 获取协议原始字节码
	 */
	public ByteBuffer buffer() {
        return buffer;
    }
	
	public int readIdx() {
		return readIdx;
	}

	public void readIdx(int readIdx) {
		this.readIdx = readIdx;
	}

	public int writeIdx() {
		return writeIdx;
	}

	public void writeIdx(int writeIdx) {
		this.writeIdx = writeIdx;
	}
	
	public void rewind() {
		readIdx = 0;
	}
	
	public void reset() {
		readIdx = 0;
		writeIdx = 0;
		buffer.position(0);
	}

	public Charset charset() {
		return charset;
	}

	public void charset(Charset charset) {
		this.charset = charset;
	}

	public byte[] array() {
        return toByteArray(buffer, readIdx, writeIdx);
    }
	
	public boolean isZigzag() {
		return zigzag;
	}

	public int getInt() throws ProtoBufException {
		return getInt(DEFAULT_KEY);
	}
	
	/**
	 * 根据Key获取整数数据
	 * 
	 * @param  key 内存Key
	 * @return 整数数据
	 * @throws ProtoBufException 解码失败时抛出此异常
	 */
	public int getInt(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.INT32.readVarValue(this);
	}
	
	public ProtoBuf putInt(int value) {
		return putInt(DEFAULT_KEY, value);
	}
	
	/**
	 * 添加整数类型
	 * 
	 * @param  key   内存Key
	 * @param  value 整数值
	 * @return 序列协议
	 */
	public ProtoBuf putInt(int key, int value) {
		ProtoFieldFactory.INT32.writeVarValue(this, key, value);
		return this;
	}
	
	public long getLong() throws ProtoBufException {
		return getLong(DEFAULT_KEY);
	}
	
	public long getLong(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.INT64.readVarValue(this);
	}
	
	public ProtoBuf putLong(long value) {
		return putLong(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putLong(int key, long value) {
		ProtoFieldFactory.INT64.writeVarValue(this, key, value);
		return this;
	}
	
	public int getByte() throws ProtoBufException {
		return getInt(DEFAULT_KEY);
	}
	
	public int getByte(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.BYTE.readVarValue(this);
	}
	
	public ProtoBuf putByte(byte value) {
		return putByte(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putByte(int key, byte value) {
		ProtoFieldFactory.BYTE.writeVarValue(this, key, value);
		return this;
	}
	
	public boolean getBoolean() throws ProtoBufException {
		return getBoolean(DEFAULT_KEY);
	}
	
	public boolean getBoolean(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.BOOLEAN.readVarValue(this);
	}
	
	public ProtoBuf putBoolean(boolean value) {
		return putBoolean(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putBoolean(int key, boolean value) {
		ProtoFieldFactory.BOOLEAN.writeVarValue(this, key, value);
		return this;
	}
	
	public short getShort() throws ProtoBufException {
		return getShort(DEFAULT_KEY);
	}
	
	public short getShort(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.SHORT.readVarValue(this);
	}
	
	public ProtoBuf putShort(short value) {
		return putShort(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putShort(int key, short value) {
		ProtoFieldFactory.SHORT.writeVarValue(this, key, value);
		return this;
	}
	
	public float getFloat() throws ProtoBufException {
		return getFloat(DEFAULT_KEY);
	}
	
	public float getFloat(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.FLOAT.readVarValue(this);
	}
	
	public ProtoBuf putFloat(float value) {
		return putFloat(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putFloat(int key, float value) {
		ProtoFieldFactory.FLOAT.writeVarValue(this, key, value);
		return this;
	}
	
	public double getDouble() throws ProtoBufException {
		return getDouble(DEFAULT_KEY);
	}
	
	public double getDouble(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.DOUBLE.readVarValue(this);
	}
	
	public ProtoBuf putDouble(double value) {
		return putDouble(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putDouble(int key, double value) {
		ProtoFieldFactory.DOUBLE.writeVarValue(this, key, value);
		return this;
	}
	
	public String getString() throws ProtoBufException {
		return getString(DEFAULT_KEY);
	}
	
	public String getString(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.STRING.readVarValue(this);
	}
	
	public ProtoBuf putString(String value) {
		return putString(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putString(int key, String value) {
		ProtoFieldFactory.STRING.writeVarValue(this, key, value);
		return this;
	}
	
	public Calendar getCalendar() throws ProtoBufException {
		return getCalendar(DEFAULT_KEY);
	}
	
	public Calendar getCalendar(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.CALENDAR.readVarValue(this);
	}
	
	public ProtoBuf putCalendar(Calendar value) {
		return putCalendar(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putCalendar(int key, Calendar value) {
		ProtoFieldFactory.CALENDAR.writeVarValue(this, key, value);
		return this;
	}
	
	public ByteBuffer getBuffer() throws ProtoBufException {
		return getBuffer(DEFAULT_KEY);
	}
	
	public ByteBuffer getBuffer(int key) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return ProtoFieldFactory.BUFFER.readVarValue(this);
	}
	
	public ProtoBuf putBuffer(ByteBuffer value) {
		return putBuffer(DEFAULT_KEY, value);
	}
	
	public ProtoBuf putBuffer(int key, ByteBuffer value) {
		ProtoFieldFactory.BUFFER.writeVarValue(this, key, value);
		return this;
	}
	
	/**
	 * 添加对象序列化
	 */
	public ProtoBuf putObject(Object value, ProtoSchema schema) throws ProtoBufException {
		return putObject(DEFAULT_KEY, value, schema);
	}
	
	public ProtoBuf putObject(int key, Object value, ProtoSchema schema) throws ProtoBufException {
		SysUtil.checkNotNull(value, "value");
		SysUtil.checkNotNull(schema, "schema");
		
		ProtoFieldFactory.OBJECT.writeVarValue(this, key, value, schema);
		return this;
	}
	
	public <T> T getObject(Class<T> typeClass, ProtoSchema schema) throws ProtoBufException {
		return getObject(DEFAULT_KEY, typeClass, schema);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getObject(int key, Class<T> typeClass, ProtoSchema schema) throws ProtoBufException {
		if (!hasReadableBytes()) {
			throw ProtoBufException.overIndex();
		}
		
		return (T) ProtoFieldFactory.OBJECT.readVarValue(this, schema);
	}
	
	/**
	 * 添加序列化Map，实现细节是将Map中的Key、Value进行序列化并逐条存储，
	 * Map中的Key因为数据是固定的，通过{@link ProtoSchema}进行转换成Int字节以节省字节数据存储和传输
	 * 
	 * @param  value  Map数据
	 * @param  schema Map元数据，在Key->Number标记将Map的Key映射为协议哪些Key值
	 * @return 序列协议
	 */
	@SuppressWarnings("unchecked")
	public ProtoBuf putMap(Map value, ProtoSchema schema) {
		return putMap(DEFAULT_KEY, value, schema);
	}
	
	@SuppressWarnings("unchecked")
	public ProtoBuf putMap(int key, Map value, ProtoSchema schema) {
		SysUtil.checkNotNull(value, "value");
		SysUtil.checkNotNull(schema, "schema");

		ProtoFieldFactory.MAP.writeVarValue(this, key, value, schema);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public Map getMap(ProtoSchema schema) throws ProtoBufException {
		return getMap(DEFAULT_KEY, schema);
	}
	
	@SuppressWarnings("unchecked")
	public Map getMap(int key, ProtoSchema schema) throws ProtoBufException {
		SysUtil.checkNotNull(schema, "schema");
		
		return ProtoFieldFactory.MAP.readVarValue(this, schema);
	}
	
	/**
	 * 添加序列化Param，实现细节是将Param中的Key、Value进行序列化并逐条存储，
	 * Map中的Key因为数据是固定的，通过{@link ProtoSchema}进行转换成Int字节以节省字节数据存储和传输
	 * 
	 * @param  value  Param数据
	 * @param  schema Param元数据，在Key->Number标记将Map的Key映射为协议哪些Key值
	 * @return 序列协议
	 */
	public ProtoBuf putParam(Param value, ProtoSchema schema) {
		return putParam(DEFAULT_KEY, value, schema);
	}
	
	public ProtoBuf putParam(int key, Param value, ProtoSchema schema) {
		SysUtil.checkNotNull(value, "value");
		SysUtil.checkNotNull(schema, "schema");

		ProtoFieldFactory.PARAM.writeVarValue(this, key, value, schema);
		return this;
	}
	
	public Param getParam(ProtoSchema schema) throws ProtoBufException {
		return getParam(DEFAULT_KEY, schema);
	}
	
	public Param getParam(int key, ProtoSchema schema) throws ProtoBufException {
		SysUtil.checkNotNull(schema, "schema");
		
		return ProtoFieldFactory.PARAM.readVarValue(this, schema);
	}
	
	public ProtoBuf putList(List<?> value, ProtoSchema schema) {
		return putList(DEFAULT_KEY, value, schema);
	}
	
	@SuppressWarnings("unchecked")
	public ProtoBuf putList(int key, List value, ProtoSchema schema) {
		SysUtil.checkNotNull(value, "value");
		SysUtil.checkNotNull(schema, "schema");
		
		ProtoFieldFactory.LIST.writeVarValue(this, key, value, schema);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public List getList(ProtoSchema schema) throws ProtoBufException {
		return getList(DEFAULT_KEY, schema);
	}
	
	@SuppressWarnings("unchecked")
	public List getList(int key, ProtoSchema schema) throws ProtoBufException {
		SysUtil.checkNotNull(schema, "schema");
		
		return ProtoFieldFactory.LIST.readVarValue(this, schema);
	}
	
	public ProtoBuf putTable(Table<?> value, ProtoSchema schema) {
		return putTable(DEFAULT_KEY, value, schema);
	}
	
	@SuppressWarnings("unchecked")
	public ProtoBuf putTable(int key, Table value, ProtoSchema schema) {
		SysUtil.checkNotNull(value, "value");
		SysUtil.checkNotNull(schema, "schema");
		
		ProtoFieldFactory.TABLE.writeVarValue(this, key, value, schema);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public Table getTable(ProtoSchema schema) throws ProtoBufException {
		return getTable(DEFAULT_KEY, schema);
	}
	
	@SuppressWarnings("unchecked")
	public Table getTable(int key, ProtoSchema schema) throws ProtoBufException {
		SysUtil.checkNotNull(schema, "schema");
		
		return ProtoFieldFactory.TABLE.readVarValue(this, schema);
	}

	/**
	 * 将数据扩容两倍
	 */
	public void expand() {
		doAutoExpand(buffer.capacity());
	}
	
	/**
	 * 压缩可用数据，剔除无用数据
	 */
	public ProtoBuf compact() {
		int total = writeIdx - readIdx;
		boolean direct = buffer.isDirect();
		ByteBuffer newBuf = null;
		if (direct) {
			newBuf = ByteBuffer.allocateDirect(total);
		} else {
			newBuf = ByteBuffer.allocate(total);
		}
		buffer.position(readIdx);
		buffer.limit(writeIdx);
		newBuf.put(buffer);
		buffer = newBuf;
		return this;
	}
	
	/**
	 * 字节数组转换为二进制字符串，每个字节以","隔开
	 */
	public String toBinaryString() {
		byte[] array = toByteArray(buffer);
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			result.append(Integer.toString(array[i] & 0xff, 2) + ",");
		}
		return result.toString().substring(0, result.length()-1);
	}
	
	/**
	 * 判断解析的序列字节是否已经结束
	 */
	public boolean hasReadableBytes() {
		return readableBytes() > 0;
	}
	
	/**
	 * 判断还能够解析的序列字节位置
	 */
	public int readableBytes() {
	    return writeIdx - readIdx;
	}
	
	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
		info.append(getClass().getSimpleName() + "[");
		info.append("readIdx=" + readIdx + " ");
		info.append("writeIdx=" + writeIdx + " ");
		info.append("capacity=" + buffer.capacity());
		info.append("]");
		return info.toString();
	}
	
	public static byte[] toByteArray(ByteBuffer buffer) {
		return toByteArray(buffer, 0, buffer.limit());
	}

	/**
	 * 将Buffer转换成字节数组，注意是从0开始的位置复制到limit结束
	 * 
	 * @param  buffer 字节缓冲
	 * @return 字节数组
	 */
	public static byte[] toByteArray(ByteBuffer buffer, int offset, int length) {
		int oldPos = buffer.position();
		int oldLimit = buffer.limit();
		try {
			byte[] bufTmp = new byte[length];
			buffer.position(offset);
			buffer.limit(offset + length);
			buffer.get(bufTmp);
			return bufTmp;
		} finally {
			buffer.limit(oldLimit);
			buffer.position(oldPos);
		}
	}
	
	/**
	 * 编码Key，用一个字节表示即可，
	 * 字节后3位存储的是传输类型{@link ProtoType}，即Value的类型，因为类型不多，3个位足够表示，
	 * 前5位存储的是Key值，即用序号1、2、3表示Key值，所以Key值不能太大，
	 * 例如doEncodeKey(1, Type.VARINT)，那么该Key在内存中的表示如下：
	 * <pre>
	 * +-------+------+
	 * |  key  | type |
	 * +-------+------+
	 * | 00001 | 001  |
	 * +-------+------+
	 * </pre>
	 * 
	 * @param key  Key值，规定都为数字类型而不用字符串，节省字节数
	 * @param type Value类型，只有知道类型才能解析对应的值
	 */
	public void doEncodeKey(int key, byte type) {
		doWriteVarInt32((key << ProtoKey.TAG_TYPE_BITS) | type);
	}
	
	/**
	 * 解码获取Key包装
	 */
	public ProtoKey doDecodeKey() {
		int value = buffer.get(readIdx++);
		// 后三位存储的是传输类型
		return new ProtoKey(value);
	}
	
	/**
	 * 将通过Protocol Buffer协议反序列化读取Int类型
	 * VarInt解码如下，
	 * <pre>
	 * +----------+----------+
	 * | 10101100 | 00000010 | 原始编码
	 * +----------+----------+
	 * |  0000010 | 0101100  | 进行解码
	 * +----------+----------+
	 * |      100101100      | 重新拼接
	 * +----------+----------+
	 * </pre>
	 */
	protected int doReadVarInt32() throws ProtoBufException {
		int value = 0;
		byte tmp = 0;
		// INT32最多可能产生5个BYTE
		for (int i = 0; i < MAX_INT32_SIZE; i++) {
			tmp = buffer.get(readIdx++);

			// 取低7位为值，同时最前面的字节表示最后的数据
			value |= ((tmp & 0x7F) << (7 * i));

			// 如果最高位为0则表示结束
			if ((tmp >>> 7) == 0) {
				return value;
			}
		}
		// 已经是第5个BYTE了，还没遇到结束符，编码可能有误
		throw ProtoBufException.malformedVarInt();
	}
	
	/**
	 * 将Int类型通过Protocol Buffer协议序列化写到内存中，
	 * VarInt编码如下，
	 * 数字每7位表示数据一部分，
	 * 最高位用来表示是否还有数据，1表示还有数据，0表示已经没有数据
	 * <pre>
	 * +---------------------------------+
	 * | 0000000000000000000000100101100 | 300的二进制，因为32位前面大部分是0，所以那些0可以压缩
	 * +---------------------------------+
	 * |    10101100    |    00000010    | 低7位取出，因为前面还有数据，所以最高位补1直到前面数据都为0
	 * +---------------------------------+
	 * </pre>
	 * 
	 * @param value Int值
	 */
	protected void doWriteVarInt32(int value) {
		// 先判断是否需要扩容，最差情况是0xffffffff，一共要用5个字节存储（最大32位按每7位拆分≈5）
		doAutoExpand(MAX_INT32_SIZE);
		
		// 循环右移7位，数据只存在低7位，最高位用于存储后续是否还有数据
		while (true) {
			writeIdx++;
			// 不断取低7位
			if ((value & ~0x7F) == 0) {
				// 剩下的右移7位，如果最高位为0，数据已经压缩完毕，直接退出
				buffer.put((byte) value);
				break;
			}
			// 最高位为1，仍然还有数据，继续添加数据
			buffer.put((byte) ((value & 0x7F) | 0x80));
			value >>>= 7;
		}
	}
	
	protected long doReadVarInt64() throws ProtoBufException {
		// INT64位最多可能产生10个bytes
		long value = 0L;
		byte tmp = 0;
		for (int i = 0; i < MAX_INT64_SIZE; i++) {
			tmp = buffer.get(readIdx++);

			// 取低7位为值，同时最前面的字节表示最后的数据
			value |= ((long) (tmp & 0x7F) << (7 * i));

			// 如果最高位为0则表示结束
			if ((tmp >>> 7) == 0) {
				return value;
			}
		}
		// 已经是第10个bytes了，还没遇到结束符，编码可能有误
		throw ProtoBufException.malformedVarInt();
	}
	
	protected void doWriteVarInt64(long value) {
		// 先判断是否需要扩容，最差情况是0xffffffff，一共要用10个字节存储
		doAutoExpand(MAX_INT64_SIZE);

		while (true) {
			writeIdx++;
			// 不断取低7位
			if ((value & ~0x7FL) == 0) {
				// 剩下的右移7位，如果最高位为0，数据已经压缩完毕，直接退出
				buffer.put((byte) value);
				break;
			}
			// 最高位为1，仍然还有数据，继续添加数据
			buffer.put((byte) ((value & 0x7F) | 0x80));
			value >>>= 7;
		}
	}
	
	/**
	 * 在空间不足情况下自动扩展空间容量，扩容为原先容量的2倍
	 */
	protected void doAutoExpand(int expectedRemaining) {
		int total = buffer.position() + expectedRemaining;
		if (total <= buffer.capacity()) {
			return;
		}

		int position = buffer.position();
		int expect = buffer.capacity() << 1;
		int size = total > expect ? total : expect;
		byte[] bufNew = new byte[size];
		byte[] bufOld = toByteArray(buffer);
		System.arraycopy(bufOld, 0, bufNew, 0, bufOld.length);
		buffer = ByteBuffer.wrap(bufNew);
		buffer.position(position);
	}
}
