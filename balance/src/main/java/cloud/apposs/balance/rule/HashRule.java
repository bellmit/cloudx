package cloud.apposs.balance.rule;

import java.util.List;

import cloud.apposs.balance.AbstractRule;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.Peer;
import cloud.apposs.logger.Logger;

/**
 * 根据指定的Key进行哈希的负载均衡策略
 */
public class HashRule extends AbstractRule {
	private static final int MAX_CYCLIC_RETRY = 3;
	
	public HashRule() {
		this(null);
	}
	
	public HashRule(ILoadBalancer balancer) {
		this.setLoadBalancer(balancer);
	}
	
	@Override
	public Peer choosePeer(Object key) {
		if (key == null) {
            Logger.warn("Invalid Hash Key");
            return null;
        }
		if (balancer == null) {
            Logger.warn("No Load Nalancer");
            return null;
        }
		
		Peer peer = null;
		int retry = 0;
		while (peer == null && retry < MAX_CYCLIC_RETRY) {
			List<Peer> upPeers = balancer.getReachablePeer();
			List<Peer> allPeers = balancer.getAllPeers();
			int upCount = upPeers.size();
	        int allCount = allPeers.size();
	        if ((upCount == 0) || (allCount == 0)) {
                Logger.warn("No Available Peer From Load Balancer: " + balancer);
                return null;
            }
			
	        int nextPeerIndex = (key.hashCode() + retry++) % allCount;
	        peer = allPeers.get(nextPeerIndex);
			if (peer == null) {
                Thread.yield();
                continue;
            }
			if (isPeerAvailable(peer)) {
                return peer;
            }
			
			peer = null;
		}
		
		if (retry >= MAX_CYCLIC_RETRY) {
			Logger.warn("No Available Alive Peer After %d Tries From Load Balancer: %s", MAX_CYCLIC_RETRY, balancer);
        }
		return peer;
	}
}
