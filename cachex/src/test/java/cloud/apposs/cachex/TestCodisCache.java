package cloud.apposs.cachex;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;
import cloud.apposs.cachex.memory.redis.RedisCache;
import cloud.apposs.util.Param;
import cloud.apposs.protobuf.ProtoSchema;

public class TestCodisCache {
	public static final String HOST = "172.17.2.55";
//	public static final String HOST = "192.168.1.5";
	public static final int PORT = 7041;
	
	private RedisCache cache = null;
	
	@Before
	public void before() {
		CacheXConfig config = new CacheXConfig();
		RedisConfig redisConfig = config.getRedisConfig();
		redisConfig.setCacheType(RedisConfig.REDIS_CACHE_CODIS);
		redisConfig.addServer(new RedisServer(HOST, PORT));
		cache = new RedisCache(config);
	}
	
	@After
	public void after() {
		cache.shutdown();
	}
	
	@Test
	public void testPutString() throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			String key = "MyKey" + i;
			cache.put(key, "MyValue" + i);
			cache.getString(key);
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testGetString() throws Exception {
		System.out.println(cache.getString("MyKey233"));
	}
	
	@Test
	public void testPutObject() throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 200; i++) {
			String key = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			System.out.println(cache.getObject(key, Product.class, schema));
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	/**
	 * 测试二级缓存存储
	 */
	@Test
	public void testHPutString() throws Exception {
		long start = System.currentTimeMillis();
		String key = "MyKey";
		for (int i = 0; i < 100; i++) {
			String field = "MyKey" + i;
			cache.hput(key, field, "MyValue" + i);
			System.out.println(cache.hgetString(key, field));
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testHPutObject() throws Exception {
		long start = System.currentTimeMillis();
		String key = "MyKey";
		for (int i = 0; i < 100; i++) {
			String field = "MyKey" + i;
			Product value = new Product(i, "MyProduct" + i);
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.hput(key, field, value, schema);
			cache.hgetObject(key, field, Product.class, schema);
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
		List<Product> products = cache.hgetAllObject(key, Product.class, ProtoSchema.getSchema(Product.class));
		System.out.println(products.size());
	}
	
	@Test
	public void testHPutParam() throws Exception {
		long start = System.currentTimeMillis();
		String key = "MyKey";
		ProtoSchema schema = ProtoSchema.mapSchema();
		schema.addKey("id", Integer.class);
		schema.addKey("name", String.class);
		schema.addKey("product", Product.class, ProtoSchema.getSchema(Product.class));
		for (int i = 0; i < 1000; i++) {
			String field = "MyKey" + i;
			Param info = new Param();
			info.setInt("id", i);
			info.setString("name", "Pvalue" + i);
			info.setObject("product", new Product(i, "中Product国" + i));
			cache.hput(key, field, info, schema);
			cache.hgetParam(key, field, schema);
		}
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
		List<Param> infoList = cache.hgetAllParam(key, schema);
		System.out.println(infoList);
	}
	
	public static class Product {
		private int id;
		
		private String name;

		public Product() {
		}
		
		public Product(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "id:" + id + ";name:" + name;
		}
	}
}
