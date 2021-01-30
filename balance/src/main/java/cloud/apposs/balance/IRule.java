package cloud.apposs.balance;

/**
 * 负载均衡策略，
 * 之所以从{@link ILoadBalancer}抽离出来在于可以让负载均衡策略不依赖于Server运行状况，
 * 专职只做策略服务定位
 */
public interface IRule {
	/**
	 * 通过负载均衡算法选择其中一台后端服务
	 */
	Peer choosePeer(Object key);
	
	void setLoadBalancer(ILoadBalancer balancer);
    
    ILoadBalancer getLoadBalancer();
}
