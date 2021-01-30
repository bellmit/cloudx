package cloud.apposs.balance.rule;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cloud.apposs.balance.AbstractRule;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.Peer;
import cloud.apposs.logger.Logger;

/**
 * 服务轮询负载均衡策略
 */
public class RoundRobinRule extends AbstractRule {
	private static final int MAX_CYCLIC_RETRY = 3;
	
	private final AtomicInteger nextNodeCyclicCounter;
	
	public RoundRobinRule() {
		this(null);
	}
	
	public RoundRobinRule(ILoadBalancer balancer) {
		this.nextNodeCyclicCounter = new AtomicInteger(0);
        setLoadBalancer(balancer);
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
			
	        int nextPeerIndex = incrementAndGetModulo(upCount);
	        peer = upPeers.get(nextPeerIndex);
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
	
	private int incrementAndGetModulo(int modulo) {
        for (;;) {
            int current = nextNodeCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextNodeCyclicCounter.compareAndSet(current, next)) {
                return next;
            }
        }
    }
}
