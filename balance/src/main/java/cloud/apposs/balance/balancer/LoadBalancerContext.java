package cloud.apposs.balance.balancer;

import cloud.apposs.balance.AbstractLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.PeerStats;

public final class LoadBalancerContext {
	private final ILoadBalancer balancer;

	public LoadBalancerContext(ILoadBalancer balancer) {
		this.balancer = balancer;
	}

	public ILoadBalancer getBalancer() {
		return balancer;
	}
	
	public final PeerStats getPeerStats(Peer peer) {
		PeerStats peerStats = null;
        if (balancer instanceof AbstractLoadBalancer){
            LoadBalancerStats lbStats = ((AbstractLoadBalancer) balancer).getLoadBalancerStats();
            peerStats = lbStats.getPeerStats(peer);
        }
        return peerStats;
	}
}
