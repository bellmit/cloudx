package cloud.apposs.cachex.memory.jvm;

import java.util.Map;
import java.util.Map.Entry;

/**
 * 缓存过期定时删除器
 */
public class JvmCacheExpirer extends Thread {
	/** 过期Key清除的时间，超过该时间则不再清除等待下次重新清除以避免大量缓存清除造成CPU繁忙 */
	public static final int PURGE_MAX_TIME = 2000;
	
	/** 定期检查间隔时间，默认为1分钟 */
	private int interval;
	
	/** JVM缓存服务 */
	private final JvmCache cache;
	
	public JvmCacheExpirer(int interval, JvmCache cache) {
		if (interval <= 0) {
			throw new IllegalArgumentException("interval");
		}
		if (cache == null) {
			throw new IllegalArgumentException("cache");
		}
		this.interval = interval;
		this.cache = cache;
		this.setDaemon(true);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
			
			doPurge();
		}
	}
	
	private int doPurge() {
		int total = 0;
		long start = System.currentTimeMillis();
		// 遍历所有一级缓存并判断缓存是否过期
		Map<String, Element0> cache0 = cache.getCache0();
		for (Entry<String, Element0> entry : cache0.entrySet()) {
			Element0 element = entry.getValue();
			boolean expired = element.isExpired();
			if (!expired) continue;
			// 缓存已经过期，直接删除缓存释放内存
			cache.removeExpired(element);
			total++;
			// 超过最大清除时间，不再清除等待下次重新清除以避免大量缓存清除造成CPU繁忙
			if (System.currentTimeMillis() - start >= PURGE_MAX_TIME) {
				return total;
			}
		}
		
		// 遍历所有二级缓存并判断缓存是否过期
		Map<String, Element1> caches1 = cache.getCache1();
		for (Entry<String, Element1> entry : caches1.entrySet()) {
			Element1 element = entry.getValue();
			boolean expired = element.isExpired();
			if (!expired) continue;
			// 缓存已经过期，直接删除缓存释放内存
			cache.removeExpired(element);
			// 超过最大清除时间，不再清除等待下次重新清除以避免大量缓存清除造成CPU繁忙
			if (System.currentTimeMillis() - start >= PURGE_MAX_TIME) {
				return total;
			}
		}
		
		return total;
	}
}
