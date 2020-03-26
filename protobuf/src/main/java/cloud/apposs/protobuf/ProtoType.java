package cloud.apposs.protobuf;

public final class ProtoType {
	/** 
	 * 值类型为正数，
	 * 即Java中的Integer/Float/Double/Short/Boolean
	 */
	public static final byte VARINT = 0;
	
	/**
	 * 值类型为长整型，即Java中的Long/Calendar
	 */
	public static final byte VARINT64 = 1;
	
	/**
	 * 值类型为定长字节，即Java中的String
	 */
	public static final byte LEN_DELIMI = 2;
	
	/**
	 * 表示接下来是一组集合，即Java中的Map/List/Object
	 */
	public static final byte GROUP_BEG = 3;
	
	/**
	 * 表示集合的结束，即Java中的Map/List
	 */
	public static final byte GROUP_END = 4;
}
