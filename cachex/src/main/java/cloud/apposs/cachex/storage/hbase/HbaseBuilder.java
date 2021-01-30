package cloud.apposs.cachex.storage.hbase;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.util.Bytes;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.HbaseConfig;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.cachex.storage.Updater;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.cachex.storage.Metadata.Column;
import cloud.apposs.cachex.storage.Where.Condition;
import cloud.apposs.util.Param;
import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.protobuf.ProtoField;
import cloud.apposs.protobuf.ProtoKey;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;
import cloud.apposs.util.StrUtil;
import cloud.apposs.protobuf.ProtoFieldFactory.ProtoFieldCodec;

public class HbaseBuilder implements SqlBuilder {
	/**
	 * {@link Where}条件查询操作符对应的HBase操作符
	 */
	private final static Map<String, CompareOp> hbaseOperations = new HashMap<String, CompareOp>();
	static {
		hbaseOperations.put(Where.EQ, CompareOp.EQUAL);
		hbaseOperations.put(Where.NE, CompareOp.NOT_EQUAL);
		hbaseOperations.put(Where.GT, CompareOp.GREATER);
		hbaseOperations.put(Where.GE, CompareOp.GREATER_OR_EQUAL);
		hbaseOperations.put(Where.LT, CompareOp.LESS);
		hbaseOperations.put(Where.LE, CompareOp.LESS_OR_EQUAL);
	}
	
	private final CacheXConfig config;
	
	private final Configuration configuration = HBaseConfiguration.create();
	
	private final HConnection connection;
	
	public HbaseBuilder(CacheXConfig config) throws IOException {
		HbaseConfig hbaseConfig = config.getHbaseConfig();
		String quorum = hbaseConfig.getQuorum();
		if (StrUtil.isEmpty(quorum)) {
			throw new IllegalArgumentException("HBaseConfig Quorum Not Configed");
		}
		this.config = config;
		configuration.set("hbase.zookeeper.property.clientPort", String.valueOf(hbaseConfig.getZkPort()));
		configuration.set("hbase.zookeeper.quorum", hbaseConfig.getQuorum());
		connection = HConnectionManager.createConnection(configuration);
	}
	
	@Override
	public String getDialect() {
		return SqlBuilder.DIALECT_HBASE;
	}
	
	@Override
	public boolean exist(Metadata metadata) throws Exception {
		HBaseAdmin admin = new HBaseAdmin(configuration);
		try {
			TableName tableName = TableName.valueOf(metadata.getTable());
			return admin.tableExists(tableName);
		} finally {
			admin.close();
		}
	}
	
	@Override
	public boolean create(Metadata metadata, boolean dropIfExist) throws Exception {
		// 新建一个数据库管理员
		HBaseAdmin admin = new HBaseAdmin(configuration);
		try {
			TableName tableName = TableName.valueOf(metadata.getTable());
			if (dropIfExist && admin.tableExists(tableName)) {
				admin.disableTable(tableName);
				admin.deleteTable(tableName);
			}
	        HTableDescriptor desc = new HTableDescriptor(tableName);
	        List<Column> columnList = metadata.getColumnList();
	        for (Column column : columnList) {
	        	// 新增数据字段
	        	String columnName = column.getName();
	        	desc.addFamily(new HColumnDescriptor(columnName));
	        }
	        admin.createTable(desc);
		} finally {
			admin.close();
		}
        return true;
	}

	/**
	 * 查询单条数据，对于Hbase来说identity即表示RowKey
	 */
	@Override
	public Entity select(String table, String primary, Object identity,
			ProtoSchema schema, Query query) throws Exception {
		// HBase中查询的RowKey不能为空
		if (identity == null) {
			throw new IllegalArgumentException("Entity Has No Identity RowKey");
		}
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		
		HTableInterface tableInf = connection.getTable(table);
		try {
			Get get = new Get(Bytes.toBytes(identity.toString()));
			Result result = tableInf.get(get);
			List<Cell> cellList = result.listCells();
			Entity entity = new Entity(primary);
			entity.setIdentity(identity);
			List<ProtoField<?>> protoFieldList = schema.getFieldList();
			for (Cell cell : cellList) {
				// 对字节数据进行反序列化
				doAddEntity(entity, cell, protoFieldList);
			}
			return entity;
		} finally {
			tableInf.close();
		}
	}
	
	@Override
	public List<Entity> query(String table, String primary,
			ProtoSchema schema, Query query) throws Exception {
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		
		HTableInterface tableInf = connection.getTable(table);
		ResultScanner scanner = null;
		try {
			Charset charset = config.getChrset();
			List<Entity> entities = new LinkedList<Entity>();
			Scan scan = new Scan();
			if (query != null && query.where() != null && !query.where().isEmpty()) {
				doBuildFilter(query.where(), scan);
			}
			scanner = tableInf.getScanner(scan);
			for (Result result : scanner) {
				List<Cell> cellList = result.listCells();
				Entity entity = new Entity(primary);
				entity.setIdentity(new String(result.getRow(), charset));
				List<ProtoField<?>> protoFieldList = schema.getFieldList();
				for (Cell cell : cellList) {
					// 对字节数据进行反序列化
					doAddEntity(entity, cell, protoFieldList);
				}
				entities.add(entity);
			}
			return entities;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			tableInf.close();
		}
	}
	
	/**
	 * 插入单条数据，对于Hbase来说identity即表示RowKey
	 */
	@Override
	public int insert(String table, Entity entity, 
			ProtoSchema schema, Ref<Object> idRef) throws Exception {
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		// HBase中插入数据的RowKey不能为空
		if (entity.getIdentity() == null) {
			throw new IllegalArgumentException("Entity Has No Identity RowKey");
		}
		
		HTableInterface tableConnection = connection.getTable(table);
		try {
			Put put = doAddPut(entity, schema);
			tableConnection.put(put);
		} finally {
			tableConnection.close();
		}
		return 1;
	}

	@Override
	public int insert(String table, List<Entity> entities, 
			ProtoSchema schema, List<Object> idList) throws Exception {
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		
		HTableInterface tableConnection = connection.getTable(table);
		try {
			List<Put> putList = new LinkedList<Put>();
			for (Entity entity : entities) {
				// HBase中插入数据的RowKey不能为空
				if (entity.getIdentity() == null) {
					throw new IllegalArgumentException("Entity Has No Identity RowKey");
				}
				putList.add(doAddPut(entity, schema));
			}
			tableConnection.put(putList);
			return putList.size();
		} finally {
			tableConnection.close();
		}
	}
	
	@Override
	public int update(String table, Entity entity, ProtoSchema schema) throws Exception {
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		// HBase中插入数据的RowKey不能为空
		if (entity.getIdentity() == null) {
			throw new IllegalArgumentException("Entity Has No Identity RowKey");
		}
		
		// 更新的操作跟添加完全一致，只不过是添加RowKey不存在，更新时RowKey已经存在
		HTableInterface tableConnection = connection.getTable(table);
		try {
			Put put = doAddPut(entity, schema);
			tableConnection.put(put);
		} finally {
			tableConnection.close();
		}
		return 1;
	}

	@Override
	public int update(String table, List<Entity> entities, ProtoSchema schema) throws Exception {
		// Schema不能为空，因为需要靠元信息进行字节序列化操作
		if (schema == null) {
			throw new IllegalArgumentException("ProtoSchema Not Specified");
		}
		
		HTableInterface tableConnection = connection.getTable(table);
		try {
			// 更新的操作跟添加完全一致，只不过是添加RowKey不存在，更新时RowKey已经存在
			List<Put> putList = new LinkedList<Put>();
			for (Entity entity : entities) {
				// HBase中插入数据的RowKey不能为空
				if (entity.getIdentity() == null) {
					throw new IllegalArgumentException("Entity Has No Identity RowKey");
				}
				putList.add(doAddPut(entity, schema));
			}
			tableConnection.put(putList);
			return putList.size();
		} finally {
			tableConnection.close();
		}
	}

	/**
	 * 数据更新，根据{@link Updater}条件更新，
	 * 需要先查询符合条件数据，然后再根据RowKey进行更新，有性能影响
	 * 
	 * @return 成功更新数据条数，更新失败返回-1
	 */
	@Override
	public int update(String table, Updater updater) throws Exception {
		HTableInterface tableConnection = connection.getTable(table);
		ResultScanner scanner = null;
		try {
			Where where = updater.where();
			Scan scan = new Scan();
			doBuildFilter(where, scan);
			scanner = tableConnection.getScanner(scan);
			List<Put> updateList = new LinkedList<Put>();
			ProtoBuf buffer = ProtoBuf.allocate();
			for (Result result : scanner) {
				byte[] rowKey = result.getRow();
				Put put = new Put(rowKey);
				for (Updater.Data data : updater.getDataList()) {
					doAddPut(put, data, buffer);
				}
				updateList.add(put);
			}
			int count = updateList.size();
			tableConnection.put(updateList);
			return count;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			tableConnection.close();
		}
	}
	
	@Override
	public int delete(String table, String primary, Object identity)
			throws Exception {
		HTableInterface tableConnection = connection.getTable(table);
		try {
			String rowKey = identity.toString();
			Delete delete = new Delete(Bytes.toBytes(rowKey));
			tableConnection.delete(delete);
			return 1;
		} finally {
			tableConnection.close();
		}
	}

	@Override
	public int delete(String table, Entity entity) throws Exception {
		HTableInterface tableConnection = connection.getTable(table);
		try {
			String rowKey = entity.getIdentity().toString();
			Delete delete = new Delete(Bytes.toBytes(rowKey));
			tableConnection.delete(delete);
			return 1;
		} finally {
			tableConnection.close();
		}
	}

	@Override
	public int delete(String table, List<Entity> entities) throws Exception {
		HTableInterface tableConnection = connection.getTable(table);
		try {
			List<Delete> deleteList = new LinkedList<Delete>();
			for (Entity entity : entities) {
				String rowKey = entity.getIdentity().toString();
				deleteList.add(new Delete(Bytes.toBytes(rowKey)));
			}
			int count = deleteList.size();
			tableConnection.delete(deleteList);
			return count;
		} finally {
			tableConnection.close();
		}
	}

	/**
	 * 数据删除，根据{@link Where}条件删除，
	 * 需要先查询符合条件数据，然后再根据RowKey进行删除，有性能影响
	 * 
	 * @return 成功删除数据条数，删除失败返回-1
	 */
	@Override
	public int delete(String table, Where where) throws Exception {
		HTableInterface tableConnection = connection.getTable(table);
		ResultScanner scanner = null;
		try {
			Scan scan = new Scan();
			doBuildFilter(where, scan);
			scanner = tableConnection.getScanner(scan);
			List<Delete> deleteList = new LinkedList<Delete>();
			for (Result result : scanner) {
				byte[] rowKey = result.getRow();
				deleteList.add(new Delete(rowKey));
			}
			int count = deleteList.size();
			tableConnection.delete(deleteList);
			return count;
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			tableConnection.close();
		}
	}

	@Override
	public List<Entity> executeQuery(String sql, String primary, Object... args) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Entity executeSelect(String sql, String primary, Object... args) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public int executeUpdate(String sql, Object... args) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
			}
		}
	}
	
	/**
	 * 将查询的值转换成Byte数组，用于HBase复杂值查询
	 */
	public static interface ValueBuilder {
		byte[] build(Object value) throws Exception;
	}
	
	private void doAddEntity(Entity entity, Cell cell, List<ProtoField<?>> protoFieldList) {
		// 对字节数据进行反序列化
		byte[] valueArray = cell.getValueArray();
		int valueOffset = cell.getValueOffset();
		int valueLength = cell.getValueLength();
		ProtoBuf buffer = ProtoBuf.wrap(valueArray, valueOffset, valueLength);
		ProtoKey protoKey = buffer.doDecodeKey();
		int keyIndex = protoKey.getKey();
		ProtoField<?> protoField = protoFieldList.get(keyIndex);
		String mapKey = (String) protoField.getField();
		ProtoFieldCodec<?> codec = protoField.getCodec();
		ProtoSchema fieldSchema = protoField.getSchema();
		Object mapValue = codec.readVarValue(buffer, protoKey.getType(), fieldSchema);
		entity.setObject(mapKey, mapValue);
	}
	
	@SuppressWarnings("unchecked")
	private Put doAddPut(Entity entity, ProtoSchema schema) {
		String rowKey = entity.getIdentity().toString();
		Put put = new Put(Bytes.toBytes(rowKey));
		
		Param value = entity.getDatas();
		List<ProtoField<?>> protoFieldList = schema.getFieldList();
		ProtoBuf buffer = ProtoBuf.allocate();
		// 对值进行序列化操作并保存到HBase中
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
			codec.writeVarValue(buffer, fieldKey, mapValue, fieldSchema);
			put.add(Bytes.toBytes(mapKey.toString()), null, buffer.array());
			buffer.reset();
		}
		return put;
	}
	
	@SuppressWarnings("unchecked")
	private void doAddPut(Put put, Updater.Data data, ProtoBuf buffer) {
		Charset charset = config.getChrset();
		byte[] family = data.getKey().getBytes(charset);
		Object value = data.getValue();
		// 任何特殊操作符都不支持
		if (!StrUtil.isEmpty(data.getOperation())) {
			throw new IllegalArgumentException("Operation['"+ data.getOperation() +"'] Not Supported");
		}
		// 更新的值必须是通过ProtoBuf进行序列化的字节码
		ProtoField<?> codec = data.getCodec();
		if (codec == null) {
			throw new IllegalArgumentException("Value['"+ value +"'] Codec Not Specified");
		}
		ProtoFieldCodec fieldCodec = codec.getCodec();
		fieldCodec.writeVarValue(buffer, codec.getKey(), value, codec.getSchema());
		put.add(family, null, buffer.array());
		buffer.reset();
	}
	
	@SuppressWarnings("unchecked")
	private void doBuildFilter(Where where, Scan scan) {
		Charset charset = config.getChrset();
		List<Filter> filters = new LinkedList<Filter>();
		List<Condition> conditionList = where.getConditionList();
		ProtoBuf buffer = ProtoBuf.allocate();
		for (Condition condition : conditionList) {
			String key = condition.getKey();
			String operation = condition.getOperation();
			Object value = condition.getValue();
			// 查询的值必须是通过ProtoBuf进行序列化的字节码
			ProtoField<?> codec = condition.getCodec();
			if (codec == null) {
				throw new IllegalArgumentException("Value['"+ value +"'] Codec Not Specified");
			}
			ProtoFieldCodec fieldCodec = codec.getCodec();
			fieldCodec.writeVarValue(buffer, codec.getKey(), value);
			// 只有部分操作符支持
			CompareOp compareOp = hbaseOperations.get(operation);
			if (compareOp == null) {
				throw new IllegalArgumentException("Operation['"+ operation +"'] Not Supported");
			}
			filters.add(new SingleColumnValueFilter(
					key.getBytes(charset), null, compareOp, buffer.array()));
			buffer.reset();
		}
		FilterList filterList = new FilterList(filters);
		scan.setFilter(filterList);
	}
}
