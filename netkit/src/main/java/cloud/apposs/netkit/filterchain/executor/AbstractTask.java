package cloud.apposs.netkit.filterchain.executor;

public abstract class AbstractTask implements Task {
	protected final long flow;
	
	protected final String group;
	
	protected final int limit;
	
	public AbstractTask(long flow, String group, int limit) {
		this.flow = flow;
		this.group = group;
		this.limit = limit;
	}

	@Override
	public long getFlow() {
		return flow;
	}

	@Override
	public String getGroup() {
		return group;
	}

	@Override
	public int getLimit() {
		return limit;
	}
}
