package cloud.apposs.netkit.filterchain.executor;

public final class ThreadPoolType {
	private final String poolGroup;
	
	private final TaskGroup taskGroup;
	
	public ThreadPoolType(String poolGroup, TaskGroup taskGroup) {
		this.poolGroup = poolGroup;
		this.taskGroup = taskGroup;
	}

	public String getPoolGroup() {
		return poolGroup;
	}

	public TaskGroup getTaskType() {
		return taskGroup;
	}
	
	public static final class TaskGroup {
		private final String group;
		
		private final int limit;

		public TaskGroup(String type) {
			this(type, -1);
		}

		public TaskGroup(String gruop, int limit) {
			this.group = gruop;
			this.limit = limit;
		}

		public String getGroup() {
			return group;
		}

		public int getLimit() {
			return limit;
		}
	}
}
