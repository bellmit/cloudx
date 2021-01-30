package cloud.apposs.balance;

/**
 * 服务实例在运行期间的动态更新，包括服务自动发现，自动剔除，
 * 可通过ZooKeeper监听回调时更新，也可通过线程定时更新
 */
public interface IPeerDiscovery {
	/**
	 * 启动服务动态更新
	 */
	void start(ILoadBalancer balancer) throws Exception;

    /**
     * 动态更新调用异常时的回调
     */
	void cause(Throwable cause);

	/**
	 * 关闭服务动态更新
	 */
	void shutdown();
}
