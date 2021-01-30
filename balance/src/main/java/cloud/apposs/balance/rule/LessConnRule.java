package cloud.apposs.balance.rule;

import java.util.List;

import cloud.apposs.balance.AbstractLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.balancer.LoadBalancerStats;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.PeerStats;
import cloud.apposs.logger.Logger;

/**
 * 最少连接数负载均衡，如果还没开启统计则降级采用 RoundRobinRule的策略选择服务实例
 */
public class LessConnRule extends RoundRobinRule {
	private LoadBalancerStats loadBalancerStats;
	
	@Override
	public Peer choosePeer(Object key) {
		if (balancer == null) {
            Logger.warn("No Load Nalancer");
            return null;
        }
		if (loadBalancerStats == null) {
            return super.choosePeer(key);
        }
		
		List<Peer> upList = balancer.getReachablePeer();
        int minimalConcurrentConnections = Integer.MAX_VALUE;
        int totalActiveRequest = 0;
        Peer chosen = null;
        for (Peer peer : upList) {
            PeerStats peerStats = loadBalancerStats.getPeerStats(peer);
            int activeRequest = peerStats.getActiveRequest();
            totalActiveRequest += activeRequest;
            if (activeRequest < minimalConcurrentConnections) {
                minimalConcurrentConnections = activeRequest;
                chosen = peer;
            }
        }
        
        // 统计还没开启，降级走轮询负载均衡
        if (chosen == null || totalActiveRequest <= 0) {
            return super.choosePeer(key);
        } else {
            return chosen;
        }
	}
	
	@Override
    public void setLoadBalancer(ILoadBalancer balancer) {
        super.setLoadBalancer(balancer);
        if (balancer instanceof AbstractLoadBalancer) {
            loadBalancerStats = ((AbstractLoadBalancer) balancer).getLoadBalancerStats();            
        }
    }
}
