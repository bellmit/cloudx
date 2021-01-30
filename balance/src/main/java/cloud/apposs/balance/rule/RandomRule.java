package cloud.apposs.balance.rule;

import java.util.List;
import java.util.Random;

import cloud.apposs.balance.AbstractRule;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.Peer;
import cloud.apposs.logger.Logger;

/**
 * 随机轮询负载均衡策略
 */
public class RandomRule extends AbstractRule {
	private static final int MAX_CYCLIC_RETRY = 3;
	
	private final Random random;
	
	public RandomRule() {
		this(null);
	}
	
	public RandomRule(ILoadBalancer balancer) {
		this.random = new Random();
		this.setLoadBalancer(balancer);
	}

	@Override
	public Peer choosePeer(Object key) {
		if (balancer == null) {
            Logger.warn("No Load Nalancer");
            return null;
        }
		
		Peer peer = null;
		int retry = 0;
		while (peer == null && retry++ < MAX_CYCLIC_RETRY) {
			List<Peer> upPeers = balancer.getReachablePeer();
			List<Peer> allPeers = balancer.getAllPeers();
			int upCount = upPeers.size();
	        int allCount = allPeers.size();
	        if ((upCount == 0) || (allCount == 0)) {
                Logger.warn("No Available Peer From Load Balancer: " + balancer);
                return null;
            }
	        
	        int index = random.nextInt(upCount);
	        peer = upPeers.get(index);
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
