package cloud.apposs.cachex.memory.redis.codis;

/**
 * Redis集群节点活跃监听服务，连接异常的Redis节点将会自动剔除，异常节点恢复时自动恢复，可以实现
 * 1、自定义线路定时检测节点是否存活并剔除无效节点
 * 2、通过ZooKeeper添加监听检测节点是否存活并剔除无效节点
 * 3、通过QConf定时获取节点监听检测节点是否存活并剔除无效节点
 */
public interface RedisWatcher {
	/**
	 * 启动集群节点监听服务
	 */
	void start();
	
	/**
	 * 关闭集群节点监听服务
	 */
	void shutdown();
}
