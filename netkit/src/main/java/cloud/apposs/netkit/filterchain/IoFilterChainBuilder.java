package cloud.apposs.netkit.filterchain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IoFilterChainBuilder {
	private final List<IoFilter> entries = new CopyOnWriteArrayList<IoFilter>();
	
	public void addFilter(IoFilter filter) {
		entries.add(filter);
	}
	
	public IoFilter getFilter(String name) {
		for (int i = 0; i < entries.size(); i++) {
        	IoFilter filter = entries.get(i);
        	if (filter.getName().equals(name)) {
        		return filter;
        	}
        }
		return null;
	}
	
	public IoFilter getFilter(Class<? extends IoFilter> filterType) {
		for (int i = 0; i < entries.size(); i++) {
        	IoFilter filter = entries.get(i);
        	if (filter.getClass().isAssignableFrom(filterType)) {
        		return filter;
        	}
        }
		return null;
	}
	
	public void buildFilterChain(IoFilterChain chain) {
        for (IoFilter filter : entries) {
            chain.add(filter);
        }
    }
	
	public void initFilterChain() {
		for (IoFilter filter : entries) {
            filter.init();
        }
	}
	
	public void destroyFilterChain() {
		for (IoFilter filter : entries) {
            filter.destroy();
        }
	}
	
	/**
     * 清除所有过滤器
     */
    public void clearFilterChain() {
    	entries.clear();
    }
}
