package cloud.apposs.balance.balancer;

import cloud.apposs.balance.Peer;
import cloud.apposs.balance.PeerStats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡状态统计服务，包括对所有后端节点的统计，
 * 权重和动态负载均衡服务均需要通过此状态来决定负载
 */
public final class LoadBalancerStats {
	private final Map<Peer, PeerStats> peerStatsInfo = new ConcurrentHashMap<Peer, PeerStats>();
	
	public void addPeerStats(Peer peer) {
		if (peer == null) {
			throw new IllegalArgumentException("peer");
		}
		peerStatsInfo.put(peer, new PeerStats(peer));
	}
	
	public PeerStats getPeerStats(Peer peer) {
		if (peer == null) {
			return null;
		}
		return doGetPeerStats(peer);
	}
	
	/**
	 * 请求开始时的统计
	 */
	public void fireRequestStart(Peer peer) {
		PeerStats peerStats = doGetPeerStats(peer);
		peerStats.incrementNumRequests();
		peerStats.incrementActiveRequestsCount();
	}
	
	/**
	 * 请求结束时的回调统计
	 */
	public void fireRequestComplete(Peer peer, long responseTime, Throwable cause) {
		PeerStats peerStats = doGetPeerStats(peer);
		peerStats.decrementActiveRequestsCount();
		peerStats.noteResponseTime(responseTime);
		if (cause != null) {
			peerStats.incrementPeerFailureCount();
		}
	}
	
	private PeerStats doGetPeerStats(Peer peer) {
		PeerStats peerStats = peerStatsInfo.get(peer);
		if (peerStats == null) {
			peerStats = new PeerStats(peer);
			peerStatsInfo.put(peer, peerStats);
		}
		return peerStats;
	}
}
