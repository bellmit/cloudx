package cloud.apposs.protobuf;

/**
 * 协议Key包装，字节码内存表现如下：
 * <pre>
 * +-------+------+
 * |  key  | type |
 * +-------+------+
 * | 00001 | 001  |
 * +-------+------+
 * </pre>
 */
public final class ProtoKey {
	/** 
	 * 协议类型存储位数
	 */
	public static final int TAG_TYPE_BITS = 3;
	/**
	 * 协议类型存储解码掩码
	 */
	public static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;
	
	/** 协议Key值，存在于字节前5位 */
	private int key;
	
	/** 协议字段值类型，存在于字节后3位 */
	private byte type;
	
	public ProtoKey(int value) {
		// 前5位存储的是key值/序号
		this.key = value >>> TAG_TYPE_BITS;
		// 后三位存储的是传输类型
		this.type = (byte) (value & TAG_TYPE_MASK);
	}
	
	/**
	 * 获取协议Key值，存在于字节前5位
	 */
	public int getKey() {
		return key;
	}
	
	/**
	 * 协议字段值类型，存在于字节后3位
	 */
	public byte getType() {
		return type;
	}
}
