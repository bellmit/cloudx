package cloud.apposs.balance.balancer;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.discovery.PollingDiscovery;
import cloud.apposs.balance.discovery.PollingDiscovery.DiscoveryAction;

/**
 * 服务自动发现，使用任务线程定期的更新后端服务列表的负载均衡
 */
public class DynamicLoadBalancer extends BaseLoadBalancer {
	public DynamicLoadBalancer(DiscoveryAction discoveryAction) {
		this(new LbConfig(), discoveryAction);
	}
	
	public DynamicLoadBalancer(LbConfig config, DiscoveryAction discoveryAction) {
		super(config);
		PollingDiscovery discovery = new PollingDiscovery(discoveryAction);
		discovery.setDaemon(config.isPollingDaemon());
		discovery.setInterval(config.getPollingInterval());
		setDiscovery(discovery);
	}
}
