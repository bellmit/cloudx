package cloud.apposs.cachex.storage;

/**
 * SQL分页查询
 */
public class Pager {
	/** 分页开始索引 */
	private int start;
	
	/** 展现条数 */
	private int limit;

	public Pager(int start, int limit) {
		this.start = start;
		this.limit = limit;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
}
