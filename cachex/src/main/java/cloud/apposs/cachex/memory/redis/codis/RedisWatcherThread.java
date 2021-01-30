package cloud.apposs.cachex.memory.redis.codis;

import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;

/**
 * 自定义线路定时检测节点是否存活并剔除无效节点，异常节点恢复时自动恢复
 */
public class RedisWatcherThread extends Thread implements RedisWatcher {
	public static final int DEFAULT_CHECK_INTERVAL = 1000;
	/** 实例检测重试次数 */
	public static final int CHECK_RETRY_TIMES = 3;
	
	private volatile boolean running;
	
	private final RedisConfig config;
	
	private final RedisPool pool;
	
	public RedisWatcherThread(RedisConfig config, RedisPool pool) {
		this.config = config;
		this.pool = pool;
	}
	
	@Override
	public void run() {
		while(running) {
			try {
				try {
					Thread.sleep(DEFAULT_CHECK_INTERVAL);
				} catch(InterruptedException e) {
					if (!running) break;
				}
				
				int connectTimeout = config.getConnectTimeout();
				int recvTimeout = config.getRecvTimeout();
				
				// 先检测存活的集群节点
				for (RedisServer server : pool.getOnlineServers()) {
					boolean alive = false;
					for (int i = 0; i < CHECK_RETRY_TIMES; i++) {
						alive = RedisConnection.checkRedisAlive(server, connectTimeout, recvTimeout);
						if (alive) {
							break;
						}
					}
					if (!alive) {
						pool.removeOnlineServer(server);
					}
				}
				
				// 再检测已故障的集群节点
				for (RedisServer server : pool.getOfflineServers()) {
					boolean alive = RedisConnection.checkRedisAlive(server, connectTimeout, recvTimeout);
					if (alive) {
						pool.addOnlineServer(server);
					}
				}
			} catch(Throwable t) {
			}
		}
	}
	
	@Override
	public void start() {
		setDaemon(true);
		super.start();
	}

	@Override
	public void shutdown() {
		this.running = false;
		interrupt();
	}
}
