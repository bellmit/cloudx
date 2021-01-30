package cloud.apposs.cachex.storage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cloud.apposs.util.Param;
import cloud.apposs.protobuf.ProtoField;
import cloud.apposs.protobuf.ProtoSchema;

/**
 * SQL UPDATE更新
 */
public class Updater {
	public static final String UPDATE_LAND = "&"; 	// logic and:&
	public static final String UPDATE_LOR = "|"; 	// logic or:|
	public static final String UPDATE_INC = "+"; 	// increase
	public static final String UPDATE_DEC = "-"; 	// decrease
	public static final String UPDATE_MUL = "*"; 	// multiply
	
	protected final Where where = new Where();
	
	protected final List<Data> dataList = new LinkedList<Data>();
	
	/**
	 * 创建更新
	 */
	public Updater() {
	}
	
	/**
	 * 创建更新
	 * 
	 * @param key 更新的字段
	 * @param value 更新的值
	 */
	public Updater(String key, Object value) {
		this(key, null, value);
	}
	
	/**
	 * 创建更新
	 * 
	 * @param key 更新的字段
	 * @param operation 更新操作，可以为{@link Updater#UPDATE_LAND}、&、+等
	 * @param value 更新的值
	 */
	public Updater(String key, String operation, Object value) {
		add(key, operation, value);
	}
	
	/**
	 * 添加更新
	 * 
	 * @param key 更新的字段
	 * @param value 更新的值
	 */
	public void add(String key, Object value) {
		add(key, null, value);
	}
	
	/**
	 * 添加更新
	 * 
	 * @param key 更新的字段
	 * @param value 更新的值
	 * @param codec 更新值对应的ProtoBuf编码解码器，如果存储的是字节则需要此元信息进行数据序列化/反序列化
	 */
	public void add(String key, Object value, ProtoField<?> codec) {
		add(key, null, value, codec);
	}
	
	/**
	 * 添加更新
	 * 
	 * @param key 更新的字段
	 * @param operation 更新操作，可以为{@link Updater#UPDATE_LAND}、&、+等
	 * @param value 更新的值
	 */
	public void add(String key, String operation, Object value) {
		dataList.add(new Data(key, operation, value, null));
	}
	
	/**
	 * 添加更新
	 * 
	 * @param key 更新的字段
	 * @param operation 更新操作，可以为{@link Updater#UPDATE_LAND}、&、+等
	 * @param value 更新的值
	 * @param codec 更新值对应的ProtoBuf编码解码器，如果存储的是字节则需要此元信息进行数据序列化/反序列化
	 */
	public void add(String key, String operation, Object value, ProtoField<?> codec) {
		dataList.add(new Data(key, operation, value, codec));
	}
	
	public void addAll(Param data) {
		for (Entry<String, Object> entry : data.entrySet()) {
			dataList.add(new Data(entry.getKey(), null, entry.getValue(), null));
		}
	}
	
	public Where where() {
		return where;
	}
	
	/**
	 * Where与查询
	 * 
	 * @param key 查询字段
	 * @param operation 查询操作，可以为=、>=、<=等操作
	 * @param value 查询的值
	 */
	public Where where(String key, String operation, Object value) {
		return where.and(key, operation, value);
	}
	
	/**
	 * Where与查询
	 * 
	 * @param key 查询字段
	 * @param operation 查询操作，可以为=、>=、<=等操作
	 * @param value 查询的值
	 * @param codec 查询值对应的ProtoBuf编码解码器，服务于存储字段为二进制
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
	 * @param schema    数据元信息，如果存储的是字节则需要此元信息进行数据序列化/反序列化
	 */
	public Where where(String key, String operation, Object value, ProtoSchema schema) {
		ProtoField<?> codec = schema.getField(key);
		return where.and(key, operation, value, codec);
	}
	
	public List<Data> getDataList() {
		return dataList;
	}

	public boolean isEmpty() {
		return dataList.isEmpty();
	}
	
	/**
	 * 更新字段封装
	 */
	public static class Data {
		/** 更新字段 */
		private final String key;
		
		/** 更新操作，等于/异或，可为空，默认为等于 */
		private final String operation;
		
		/** 更新的值 */
		private final Object value;
		
		/** 要更新字段数值编码解码器，主要服务于NOSQL二进制 */
		private final ProtoField<?> codec;

		public Data(String key, String operation, Object value, ProtoField<?> codec) {
			this.key = key;
			this.operation = operation;
			this.value = value;
			this.codec = codec;
		}

		public String getKey() {
			return key;
		}

		public String getOperation() {
			return operation;
		}

		public Object getValue() {
			return value;
		}

		public ProtoField<?> getCodec() {
			return codec;
		}
	}
}
