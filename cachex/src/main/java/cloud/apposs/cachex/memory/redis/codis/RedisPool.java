package cloud.apposs.cachex.memory.redis.codis;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import cloud.apposs.cachex.CacheXConfig.RedisConfig;
import cloud.apposs.cachex.CacheXConfig.RedisConfig.RedisServer;
import cloud.apposs.util.SysUtil;

/**
 * Redis连接池
 */
public class RedisPool implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Redis配置 */
	private final RedisConfig config;
	
	/** 数据库连接池是否正在运行 */
	private volatile boolean running = true;
	
	/** 连接池同步锁 */
    private final ReentrantLock mainLock = new ReentrantLock();
	
	/** 存活的Redis服务器/代理服务器 */
	private final List<RedisServer> onlineServers = new CopyOnWriteArrayList<RedisServer>();
	/** 离线的Redis服务器/代理服务器 */
	private final List<RedisServer> offlineServers = new CopyOnWriteArrayList<RedisServer>();
	
	/** 所有空闲的Redis连接 */
	private final Map<RedisServer, RedisConnectionPartition> connections = 
		new ConcurrentHashMap<RedisServer, RedisConnectionPartition>();
	
	/**
	 * Redis集群健康监控服务
	 */
	private final RedisWatcher watcher;
	
	/**
	 * 各Connection连接监听，包括连接满，连接异常等
	 */
	private final RedisConnectionListenerSupport listeners = new RedisConnectionListenerSupport();
	
	private final Random random = new Random();
	
	public RedisPool(RedisConfig config) {
		SysUtil.checkNotNull(config, "config");
		this.config = config;
		// 初始化各个Connection分区
		List<RedisServer> serverList = config.getServerList();
		int connectTimeout = config.getConnectTimeout();
		int recvTimeout = config.getRecvTimeout();
		for (RedisServer server : serverList) {
			// 检测可用的Redis服务列表并添加到onlineServers中
			if (!server.isOnline()) {
				continue;
			}
			boolean alive = RedisConnection.checkRedisAlive(server, connectTimeout, recvTimeout);
			if (alive) {
				addOnlineServer(server);
			} else {
				removeOnlineServer(server);
			}
		}
		// 初始化连接监听服务
		this.watcher = RedisWatcherFactory.getRedisWatcher(config, this);
		this.watcher.start();
	}
	
	public RedisConfig getConfig() {
		return config;
	}

	public List<RedisServer> getOnlineServers() {
		return onlineServers;
	}

	public List<RedisServer> getOfflineServers() {
		return offlineServers;
	}
	
	/**
	 * Redis服务已经在线，重新添加服务到连接池分区中
	 * 
	 * @param onlineServer 已经在线的Redis服务
	 */
	public synchronized void addOnlineServer(RedisServer onlineServer) {
		onlineServers.add(onlineServer);
		offlineServers.remove(onlineServer);
		if (!connections.containsKey(onlineServer)) {
			RedisConnectionPartition partition = new RedisConnectionPartition(this);
			connections.put(onlineServer, partition);
		}
	}
	
	/**
	 * Redis服务已经离线，从连接池中移除指定服务连接池
	 * 
	 * @param offlineServer 离线的Redis服务
	 */
	public synchronized void removeOnlineServer(RedisServer offlineServer) {
		onlineServers.remove(offlineServer);
		offlineServers.add(offlineServer);
		RedisConnectionPartition partition = connections.remove(offlineServer);
		if (partition != null) {
			partition.destroyAllConnections();
		}
	}

	public RedisConnectionListenerSupport getListeners() {
		return listeners;
	}

	/**
	 * 获取可用Redis连接
	 */
	public RedisConnection getConnection() {
		// 判断连接池是否已经关闭
		if (!running){
			throw new IllegalStateException("Redis Pool Has Been Shutdown");
		}
		// 一个可用的在线服务都没有
		if (onlineServers.isEmpty()) {
			throw new IllegalStateException("No Available Redis Server Error");
		}
		
		// 获取分区
		// 获取当前线程ID，按分区数量对该值取模，计算出要访问的分区数组下标
		int partitionIndex = (int) (Thread.currentThread().getId() % onlineServers.size());
		RedisServer selectedServer = onlineServers.get(partitionIndex);
		RedisConnectionPartition partition = connections.get(selectedServer);
		// 如果不存在该分区，说明该分区的Redis服务连接已经失效被监听服务给移除了，
		// 那么从分区数组的0号开始轮询每个分区，直到获取可用分区
		if (partition == null) {
			// 遍历所有分区
			for (int i = 0; i < onlineServers.size(); i++){
				if (i == partitionIndex) {
					continue;
				}
				selectedServer = onlineServers.get(i);
				partition = connections.get(selectedServer);
				// 找到了
				if (partition != null) {
					partitionIndex = i;
					break;
				}
			}
		}
		
		// 获取分区空闲连接
		RedisConnection connection = partition.borrowConnection();
		// 如果poll的结果为null，说明该分区的队列中没有空闲的connection
		// 那么从分区数组的0号开始轮询每个分区，直到poll出一个非null的connection
		if (connection == null) {
			// 遍历所有分区
			for (int i = 0; i < onlineServers.size(); i++){
				selectedServer = onlineServers.get(i);
				partition = connections.get(selectedServer);
				if (partition == null) {
					continue;
				}
				connection = partition.borrowConnection();
				// 找到了
				if (connection != null) {
					break;
				}
			}
		}
		
		// 如果遍历所有Connection分区仍为null
		// 在connection分区没有达到了Connection连接上限的情况下
		// 向队列中插入一个指定RedisConfig#acquireIncrement数量的connection
		if (connection == null) {
			for (int i = 0; i < onlineServers.size(); i++){
				selectedServer = onlineServers.get(i);
				partition = connections.get(selectedServer);
				if (partition == null) {
					continue;
				}
				if (partition.addIfUnderMaxConnectons()) {
					connection = partition.borrowConnection();
					break;
				}
			}
		}
		
		// 已经没有可用连接，直接返回空
		// 不阻塞等待，避免影响服务性能反而导致请求堆积造成雪崩
		if (connection == null) {
			return null;
		}
		
		// 激活Connection连接
		connection.active();
		
		// 记录操作跟踪日志
		if (config.isPoolOperationWatch()) {
			StringBuilder stringBuilder = new StringBuilder();
			// 记录Connection操作轨迹
			StackTraceElement[] trace = Thread.currentThread().getStackTrace();
			for(int i = 0; i < trace.length; i++){
				stringBuilder.append(" ").append(trace[i]).append("\r\n");
			}

			partition.addConnectionStackTrace(connection, stringBuilder.toString());
		}
		
		return connection;
	}
	
	/**
	 * 返回所有创建的Connection连接数
	 * 
	 * @return 所有创建的Connection连接数
	 */
	public int getNumCreatedConnections() {
		ReentrantLock lock = this.mainLock;
		lock.lock();
		try {
			int total=0;
			for (RedisConnectionPartition partition : connections.values()){
				total += partition.getNumCreatedConnections();
			}
			return total;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 获取该分区所有可用的Connection连接数
	 * 
	 * @return 所有可用的Connection连接数
	 */
	public int getNumIdleConnections() {
		ReentrantLock lock = this.mainLock;
		lock.lock();
		try {
			int total=0;
			for (RedisConnectionPartition partition : connections.values()){
				total += partition.getNumIdleConnections();
			}
			return total;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 返回所有忙碌Connection连接数（即被应用程序获取并使用Connection）
	 * 
	 * @return 所有忙碌Connection连接数
	 */
	public int getNumBusyConnections() {
		ReentrantLock lock = this.mainLock;
		lock.lock();
		try {
			int total=0;
			for (RedisConnectionPartition partition : connections.values()){
				total += partition.getNumCreatedConnections() - partition.getNumIdleConnections();
			}
			return total;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 关闭连接池
	 */
	public synchronized void shutdown(){
		if (!running) {
			return;
		}
		running = false;
		watcher.shutdown();
		terminateAllConnections();
	}
	
	@Override
	public String toString() {
		StringBuffer info = new StringBuffer();
		info.append("{");
		info.append("numCreated:").append(getNumCreatedConnections()).append(", ");
		info.append("numIdle:").append(getNumIdleConnections()).append(", ");
		info.append("numBusy:").append(getNumBusyConnections());
		info.append("}");
		return info.toString();
	}
	
	/**
	 * 关闭所有Redis连接
	 */
	public void terminateAllConnections(){
		mainLock.lock();
		try{
			// 销毁所有Connection连接
			for (RedisConnectionPartition partition : connections.values()){
				partition.destroyAllConnections();
			}
		} finally {
			mainLock.unlock();
		}
	}
	
	/**
	 * 随机选择集群配置中的Redis服务器
	 */
	protected RedisServer selectRandomServer() {
		int size = onlineServers.size();
		return onlineServers.get(random.nextInt(size));
	}
}
