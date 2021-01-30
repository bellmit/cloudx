package cloud.apposs.balance;

public abstract class AbstractRule implements IRule {
	protected ILoadBalancer balancer;

	@Override
	public ILoadBalancer getLoadBalancer() {
		return balancer;
	}

	@Override
	public void setLoadBalancer(ILoadBalancer balancer) {
		this.balancer = balancer;
	}
	
	protected boolean isPeerAvailable(Peer peer) {
		return balancer.isPeerAlive(peer) && peer.isReady();
	}
}
