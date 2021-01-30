package cloud.apposs.cachex;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.cachex.storage.Updater;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.cachex.storage.jdbc.MysqlBuilder;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

public class TestHbaseDao {
	public static final String TABLE = "user";
	public static final String DIALECT = SqlBuilder.DIALECT_HBASE;
	public static final Object ROWKEY = "MyRowKey001";
	
	private Dao dao;
	
	@Before
	public void before() throws Exception {
		CacheXConfig config = new CacheXConfig();
		config.setDialect(DIALECT);
		config.getHbaseConfig().setQuorum("172.17.1.225");
		dao = new Dao(config);
	}
	
	@After
	public void after() throws Exception {
		if (dao != null) {
			dao.shutdown();
		}
	}
	
	@Test
	public void testBuilder() throws Exception {
		MysqlBuilder builder = (MysqlBuilder) dao.getBuilder();
		System.out.println(builder.generateQuerySql(TABLE, null, null, null));
	}
	
	@Test
	public void testExists() throws Exception {
		Metadata metadata = new Metadata("user");
		System.out.println(dao.exist(metadata));
	}
	
	@Test
	public void testCreate() throws Exception {
		Metadata metadata = new Metadata("user");
		metadata.addColumn("id", Metadata.COLUMN_TYPE_INT);
		metadata.addColumn("name", Metadata.COLUMN_TYPE_STRING);
		metadata.addColumn("class", Metadata.COLUMN_TYPE_STRING);
		TestCase.assertTrue(dao.create(metadata, true));
	}
	
	@Test
	public void testSelect() throws Exception {
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		Entity datas = dao.select(TABLE, ROWKEY, schema);
		System.out.println(datas);
	}
	
	@Test
	public void testQuery() throws Exception {
		long start = System.currentTimeMillis();
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		List<Entity> datas = dao.query(TABLE, schema);
		System.out.println(datas);
		System.out.println("Execute Time:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testQueryWhere() throws Exception {
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		Query query = new Query();
		query.where("name", Where.EQ, "way102", schema);
		List<Entity> datas = dao.query(TABLE, schema, query);
		System.out.println(datas);
	}
	
	@Test
	public void testUpdate() throws Exception {
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		Entity e0 = new Entity("id");
		e0.setIdentity("MyRowKey001");
		e0.setInt("class", 110);
		e0.setString("name", "wayken100");
		int count = dao.update(TABLE, e0, schema);
		TestCase.assertTrue(count > 0);
	}
	
	@Test
	public void testUpdateBatch() throws Exception {
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		Entity e0 = new Entity("id");
		e0.setIdentity("MyRowKey001");
		e0.setString("name", "wayken0");
		Entity e1 = new Entity("id");
		e1.setIdentity("MyRowKey002");
		e1.setString("name", "wayken1");
		List<Entity> entities = new LinkedList<Entity>();
		entities.add(e0);
		entities.add(e1);
		int count = dao.update(TABLE, entities, schema);
		TestCase.assertTrue(entities.size() == count);
	}
	
	@Test
	public void testUpdater() throws Exception {
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		
		Updater u = new Updater();
		u.add("class", 114, schema.getField("class"));
		u.where("name", Where.EQ, "way100", schema.getField("name"));
		int count = dao.update(TABLE, u);
		System.out.println(count);
	}
	
	@Test
	public void testInsert() throws Exception {
		Entity e = new Entity("id");
		e.setIdentity("MyRowKey001");
		e.setInt("class", 100);
		e.setString("name", "way100");
		Ref<Object> idRef = new Ref<Object>();
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		int count = dao.insert(TABLE, e, schema, idRef);
		TestCase.assertTrue(count > 0);
	}
	
	@Test
	public void testInsertBatch() throws Exception {
		Entity e0 = new Entity("id");
		e0.setIdentity("MyRowKey001");
		e0.setInt("class", 100);
		e0.setString("name", "way100");
		Entity e1 = new Entity("id");
		e1.setIdentity("MyRowKey002");
		e1.setInt("class", 102);
		e1.setString("name", "way102");
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("class", Integer.class);
		List<Entity> all = new LinkedList<Entity>();
		all.add(e0);
		all.add(e1);
		int count = dao.insert(TABLE, all, schema);
		TestCase.assertTrue(all.size() == count);
	}
	
	@Test
	public void testDelete() throws Exception {
		Entity e = new Entity("id");
		e.setIdentity("MyRowKey001");
		int count = dao.delete(TABLE, e);
		TestCase.assertTrue(count > 0);
	}
	
	@Test
	public void testDeleteWhere() throws Exception {
		Where where = new Where("id", Where.EQ, 17);
		int count = dao.delete(TABLE, where);
		System.out.println(count);
	}
	
	@Test
	public void testDeleteBatch() throws Exception {
		Entity e0 = new Entity("id");
		e0.setIdentity("MyRowKey001");
		Entity e1 = new Entity("id");
		e1.setIdentity("MyRowKey002");
		List<Entity> all = new LinkedList<Entity>();
		all.add(e0);
		all.add(e1);
		int count = dao.delete(TABLE, all);
		System.out.println(count);
	}
}
