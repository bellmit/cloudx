package cloud.apposs.balance.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cloud.apposs.balance.AbstractLoadBalancer;
import cloud.apposs.balance.balancer.LoadBalancerStats;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.PeerStats;
import cloud.apposs.logger.Logger;

/**
 * 基于请求响应时间加权计算的规则，
 * 每30秒计算一次服务器响应时间，以响应时间作为权重，响应时间越短的服务器被选中的概率越大
 * 如果此规则没有生效，将采用 RoundRobinRule的策略选择服务实例
 * 参考：
 * https://mp.weixin.qq.com/s/3ZL4pSwN2nFEHJBWKva8nA
 */
public class FairRule extends RoundRobinRule {
	private static final int WEIGHT_TIMER_INTERVAL = 30 * 1000;
	
	private volatile List<Double> accumulatedWeights = new ArrayList<Double>();
	
	private volatile double maxWeights = 0.0;
	
	private final Random random = new Random();
	
	public FairRule() {
		Thread timer = new ServerWeightTimer();
		timer.setDaemon(true);
		timer.start();
	}
	
	@Override
	public Peer choosePeer(Object key) {
		if (balancer == null) {
            Logger.warn("No Load Nalancer");
            return null;
        }
		
		Peer peer = null;
		while(peer == null) {
			final List<Double> currentWeights = accumulatedWeights;
			final double currentMaxWeights = maxWeights;
			final List<Peer> upList = balancer.getReachablePeer();
			int upListSize = upList.size();
			if (upListSize <= 0) {
				return null;
			}
			
			if (currentMaxWeights < 0.001d || upListSize != currentWeights.size()) {
				// 响应统计还没触发，降级为轮询查询
				peer = super.choosePeer(key);
			} else {
				int serverIndex = 0;
				double randomWeight = random.nextDouble() * currentMaxWeights;
                for (int i = 0; i < currentWeights.size(); i++) {
                	double weight = currentWeights.get(i);
                	// 响应时间越短权重越大，越有可能命中
                    if (weight >= randomWeight) {
                        serverIndex = i;
                        break;
                    }
                }

                peer = upList.get(serverIndex);
			}
			
			if (peer == null) {
                Thread.yield();
                continue;
            }
			if (isPeerAvailable(peer)) {
                return peer;
            }
			
			peer = null;
		}
		
		return peer;
	}

	private void setWeights(List<Double> weights) {
        this.accumulatedWeights = weights;
    }
	
	private void setMaxWeights(double maxWeights) {
		this.maxWeights = maxWeights;
	}

	class ServerWeightTimer extends Thread {
		@Override
		public void run() {
			while(true) {
				try {
					LoadBalancerStats stats = ((AbstractLoadBalancer) balancer).getLoadBalancerStats();
					if (stats == null) {
						return;
					}
					
					// 将所有节点的平时耗时进行累加
					double maxResponseTime = 0.0;
					double totalResponseTime = 0.0;
					for (Peer peer : balancer.getReachablePeer()) {
						PeerStats stat = stats.getPeerStats(peer);
						totalResponseTime = stat.getResponseTimeAvg();
						if (maxResponseTime < totalResponseTime) {
							maxResponseTime = totalResponseTime;
						}
					}
					
					// 累加的所有时间耗时减去节点的平时耗时就是节点的权重，
					// 即表示节点耗时越低权重越高
					List<Double> finalWeights = new ArrayList<Double>();
					for (Peer peer : balancer.getReachablePeer()) {
						PeerStats stat = stats.getPeerStats(peer);
	                    double weight = totalResponseTime - stat.getResponseTimeAvg();
	                    finalWeights.add(weight);   
	                }
					setWeights(finalWeights);
					setMaxWeights(maxResponseTime);
					
					try {
						Thread.sleep(WEIGHT_TIMER_INTERVAL);
					} catch (InterruptedException e) {
					}
				} catch(Exception e) {
					Logger.error(e, "Error Calculating Server Weights");
				}
			}
		}
	}
}
