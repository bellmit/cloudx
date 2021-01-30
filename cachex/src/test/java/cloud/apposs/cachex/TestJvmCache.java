package cloud.apposs.cachex;

import java.util.List;

import org.junit.Test;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.JvmConfig;
import cloud.apposs.cachex.memory.Cache;
import cloud.apposs.cachex.memory.jvm.Element;
import cloud.apposs.cachex.memory.jvm.JvmCache;
import cloud.apposs.cachex.memory.jvm.JvmCacheListenerAdapter;
import cloud.apposs.protobuf.ProtoSchema;

/**
 * -Xms100M -Xmx100M -Xmn10M -XX:+PrintGCDateStamps -XX:+PrintGCDetails
 */
public class TestJvmCache {
	@Test
	public void testPutString() throws Exception {
		CacheXConfig config = new CacheXConfig();
		Cache cache = new JvmCache(config);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 2; i++) {
			String key = "MyKey" + i;
			System.out.println(cache.put(key, "MyValue"));
			cache.getString(key);
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testPutObject() throws Exception {
		CacheXConfig config = new CacheXConfig();
		Cache cache = new JvmCache(config);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 200000; i++) {
			String key = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			cache.getObject(key, Product.class, schema);
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testPutObject2() throws Exception {
		CacheXConfig config = new CacheXConfig();
		Cache cache = new JvmCache(config);
		int i = 0;
		while(true) {
			String key = "MyKey" + i++;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			cache.getObject(key, Product.class, schema);
			System.out.println(cache);
		}
	}
	
	/**
	 * 测试当缓存超过指定条数时的回收策略
	 */
	@Test
	public void testPutObject3() throws Exception {
		CacheXConfig config = new CacheXConfig();
		JvmConfig jvmConfig = config.getJvmConfig();
		jvmConfig.setMaxElements(100);
		JvmCache cache = new JvmCache(config);
		cache.addListener(new MyListener());
		for (int i = 0; i < 200; i++) {
			String key = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			cache.getObject(key, Product.class, schema);
		}
		System.out.println(cache.size());
	}
	
	/**
	 * 测试当缓存超过指定内存容量时的回收策略
	 */
	@Test
	public void testPutObject4() throws Exception {
		CacheXConfig config = new CacheXConfig();
		JvmConfig jvmConfig = config.getJvmConfig();
//		jvmConfig.setEvictionPolicy(null);
//		jvmConfig.setConcurrencyLevel(256);
		jvmConfig.setMaxMemory(1024 * 1024 * 30);
		JvmCache cache = new JvmCache(config);
		cache.addListener(new MyListener());
		long start = System.currentTimeMillis();
		for (int i = 0; i < 400000; i++) {
			String key = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			cache.getObject(key, Product.class, schema);
			System.out.println(cache);
		}
		System.out.println("batch execute4:" + (System.currentTimeMillis() - start));
	}
	
	/**
	 * 测试当缓存过期时间随机
	 */
	@Test
	public void testPutObject5() throws Exception {
		CacheXConfig config = new CacheXConfig();
		JvmConfig jvmConfig = config.getJvmConfig();
		jvmConfig.setExpirationTimeRandom(true);
		jvmConfig.setExpirationTimeRandomMin(10 * 1000);
		jvmConfig.setExpirationTimeRandomMax(30 * 1000);
		JvmCache cache = new JvmCache(config);
		cache.addListener(new MyListener());
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			String key = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
			ProtoSchema schema = ProtoSchema.getSchema(Product.class);
			cache.put(key, value, schema);
			cache.getObject(key, Product.class, schema);
//			System.out.println(cache);
		}
		System.out.println("batch execute4:" + (System.currentTimeMillis() - start));
	}
	
	/**
	 * 测试二级缓存存储
	 */
	@Test
	public void testHPutString() throws Exception {
		long start = System.currentTimeMillis();
		CacheXConfig config = new CacheXConfig();
		Cache cache = new JvmCache(config);
		String key = "MyKey";
		for (int i = 0; i < 100000; i++) {
			String field = "MyKey" + i;
			cache.hput(key, field, "MyValue");
			cache.hgetString(key, field);
		}
		System.out.println(cache.size());
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
	}
	
	@Test
	public void testHPutObject() throws Exception {
		CacheXConfig config = new CacheXConfig();
		Cache cache = new JvmCache(config);
		long start = System.currentTimeMillis();
		String key = "MyKey";
		for (int i = 0; i < 100000; i++) {
			String field = "MyKey" + i;
			Product value = new Product(i, "MyProduct");
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
	public void testHPutObjectMaxMemory() throws Exception {
		CacheXConfig config = new CacheXConfig();
		JvmConfig jvmConfig = config.getJvmConfig();
		jvmConfig.setMaxMemory(1024 * 1024 * 30);
		JvmCache cache = new JvmCache(config);
		long start = System.currentTimeMillis();
		for (int i = 0; i < 5000; i++) {
			String key = "MyKey" + i;
			for (int j = 0; j < 100; j++) {
				String field = "MyKey" + j;
				Product value = new Product(j, "MyProduct");
				ProtoSchema schema = ProtoSchema.getSchema(Product.class);
				cache.hput(key, field, value, schema);
				cache.hgetObject(key, field, Product.class, schema);
			}
		}
		System.out.println(cache);
		System.out.println("batch execute:" + (System.currentTimeMillis() - start));
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
	
	public static class MyListener extends JvmCacheListenerAdapter {
		private int count = 0;
		
		@Override
		public void cacheEvicted(String key, Element value) {
//			System.out.println("cache:" + key + " evict" + count++);
		}

		public int getCount() {
			return count;
		}
	}
}
