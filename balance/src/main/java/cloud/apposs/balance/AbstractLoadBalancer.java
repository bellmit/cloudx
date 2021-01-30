package cloud.apposs.balance;

import cloud.apposs.balance.balancer.LoadBalancerStats;

public abstract class AbstractLoadBalancer implements ILoadBalancer {
	public abstract LoadBalancerStats getLoadBalancerStats();
}
