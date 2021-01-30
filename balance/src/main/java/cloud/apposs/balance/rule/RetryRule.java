package cloud.apposs.balance.rule;

import cloud.apposs.balance.AbstractRule;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IRule;
import cloud.apposs.balance.Peer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 在指定时间内重试可用服务的负载
 */
public class RetryRule extends AbstractRule {
	private static final int DEFAULT_MAX_RETRY_MILLIS = 500;
	
	private IRule subRule = new RoundRobinRule();
	
	private long maxRetryMillis = DEFAULT_MAX_RETRY_MILLIS;
	
	public RetryRule() {
		this(null, null, DEFAULT_MAX_RETRY_MILLIS);
	}
	
	public RetryRule(ILoadBalancer balancer, IRule subRule, long maxRetryMillis) {
		this.subRule = (subRule != null) ? subRule : new RoundRobinRule();
		this.maxRetryMillis = (maxRetryMillis > 0) ? maxRetryMillis : DEFAULT_MAX_RETRY_MILLIS;
		this.setLoadBalancer(balancer);
	}
	
	public void setRule(IRule subRule) {
		if (subRule != null) {
			this.subRule = subRule;
		}
	}
	
	public void setMaxRetryMillis(long maxRetryMillis) {
		if (maxRetryMillis > 0) {
			this.maxRetryMillis = maxRetryMillis;
		}
	}
	
	@Override
	public void setLoadBalancer(ILoadBalancer balancer) {		
		super.setLoadBalancer(balancer);
		subRule.setLoadBalancer(balancer);
	}
	
	@Override
	public Peer choosePeer(Object key) {
		long requestTime = System.currentTimeMillis();
		long deadline = requestTime + maxRetryMillis;

		Peer peer = subRule.choosePeer(key);

		if (((peer == null) || (!peer.isAlive()))
				&& (System.currentTimeMillis() < deadline)) {
			InterruptTask task = new InterruptTask(deadline - System.currentTimeMillis());
			while (!Thread.interrupted()) {
				peer = subRule.choosePeer(key);
				if (((peer == null) || (!peer.isAlive())) || (!peer.isReady())
						&& (System.currentTimeMillis() < deadline)) {
					Thread.yield();
				} else {
					break;
				}
			}

			task.cancel();
		}

		if ((peer == null) || (!peer.isAlive()) || (!peer.isReady())) {
			return null;
		} else {
			return peer;
		}
	}
	
	static class InterruptTask extends TimerTask {
		private static Timer timer = new Timer("InterruptTimer", true); 
		
		private Thread target = null;

		public InterruptTask(long millis) {
			target = Thread.currentThread();
			timer.schedule(this, millis);
		}

		public InterruptTask(Thread target, long millis) {
			this.target = target;
			timer.schedule(this, millis);
		}
		public boolean cancel() {
			try {
				return super.cancel();
			} catch (Exception e) {
				return false;
			}
		}

		public void run() {
			if ((target != null) && (target.isAlive())) {
				target.interrupt();
			}
		}
	}
}
