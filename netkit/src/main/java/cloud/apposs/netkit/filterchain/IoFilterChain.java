package cloud.apposs.netkit.filterchain;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.*;
import cloud.apposs.netkit.filterchain.IoFilter.NextFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IoFilter}责任链
 */
public class IoFilterChain {
	private final Map<String, Entry> entryMap = new ConcurrentHashMap<String, Entry>();

	private final IoProcessor processor;
	
	/** 链头 */
    private final Entry head;

    /** 链尾 */
    private final Entry tail;
    
    public IoFilterChain(IoProcessor processor) {
    	this.processor = processor;
    	head = new Entry(new HeadFilter());
        tail = new Entry(new TailFilter());
        head.nextEntry = tail;
        tail.prevEntry = head;
    }
	
	public Entry getEntry(String name) {
		Entry e = entryMap.get(name);
        return e;
	}
	
	public Entry getEntry(IoFilter filter) {
		Entry e = head.nextEntry;
		// 遍历责任链所有节点进行查找匹配
        while (e != tail) {
            if (e.getFilter() == filter) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
	}
	
	public Entry getEntry(Class<? extends IoFilter> filterType) {
		Entry e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                return e;
            }
            e = e.nextEntry;
        }
        return null;
	}
	
	public IoFilter get(String name) {
		Entry e = getEntry(name);
        if (e == null) {
            return null;
        }
        return e.getFilter();
	}
	
	public IoFilter get(Class<? extends IoFilter> filterType) {
		Entry e = getEntry(filterType);
        if (e == null) {
            return null;
        }
        return e.getFilter();
	}
	
	public NextFilter getNextFilter(String name) {
		Entry e = getEntry(name);
        if (e == null) {
            return null;
        }
        return e.getNextFilter();
	}
	
	public NextFilter getNextFilter(IoFilter filter) {
		Entry e = getEntry(filter);
        if (e == null) {
            return null;
        }
        return e.getNextFilter();
	}
	
	public NextFilter getNextFilter(Class<? extends IoFilter> filterType) {
		Entry e = getEntry(filterType);
	        if (e == null) {
	            return null;
	        }
	        return e.getNextFilter();
	}
	
	/**
	 * 获取责任链所有节点列表
	 */
	public List<Entry> getAll() {
		List<Entry> list = new ArrayList<Entry>();
        Entry e = head.nextEntry;
        while (e != tail) {
            list.add(e);
            e = e.nextEntry;
        }
        return list;
	}
	
	public boolean contains(String name) {
		return getEntry(name) != null;
	}
	
	public boolean contains(IoFilter filter) {
		return getEntry(filter) != null;
	}
	
	public boolean contains(Class<? extends IoFilter> filterType) {
        return getEntry(filterType) != null;
    }
	
	/**
	 * 链式添加各种{@link IoFilter}，默认添加到链表末尾（tail之前）
	 */
	public void add(IoFilter filter) {
		checkAddable(filter);
		register(filter);
	}
	
	public IoFilter remove(String name) {
		Entry entry = checkRemovable(name);
        deregister(entry);
        return entry.getFilter();
	}
	
	public void remove(IoFilter filter) {
		Entry e = head.nextEntry;
        while (e != tail) {
            if (e.getFilter() == filter) {
                deregister(e);
                return;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + filter.getClass().getName());
	}

    public IoFilter remove(Class<? extends IoFilter> filterType) {
    	Entry e = head.nextEntry;
        while (e != tail) {
            if (filterType.isAssignableFrom(e.getFilter().getClass())) {
                IoFilter oldFilter = e.getFilter();
                deregister(e);
                return oldFilter;
            }
            e = e.nextEntry;
        }
        throw new IllegalArgumentException("Filter not found: "
                + filterType.getName());
    }

    /**
     * 清除所有过滤器
     */
    public void clear() {
    	List<Entry> entries =
    			new ArrayList<Entry>(entryMap.values());
        for (Entry entry : entries) {
            deregister(entry);
        }
    }
    
    public void fireChannelAccept(EventChannel channel) throws Exception {
        Entry head = this.head;
        callNextFilterChannelAccept(head, processor, channel);
    }

    private void callNextFilterChannelAccept(Entry entry,
    		IoProcessor processor, EventChannel channel) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelAccept(nextFilter, processor, channel);
    }
    
    public void fireChannelConnect() throws Exception {
        Entry head = this.head;
        callNextFilterChannelConnect(head, processor);
    }

    private void callNextFilterChannelConnect(Entry entry, 
    		IoProcessor processor) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelConnect(nextFilter, processor);
    }
    
    public void fireChannelRead(Object msg) throws Exception {
        Entry head = this.head;
        callNextFilterChannelRead(head, processor, msg);
    }

    private void callNextFilterChannelRead(Entry entry, 
    		IoProcessor processor, Object msg) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelRead(nextFilter, processor, msg);
    }
    
    public void fireChannelReadEof(Object msg) throws Exception {
        Entry head = this.head;
        callNextFilterChannelReadEof(head, processor, msg);
    }
    
    private void callNextFilterChannelReadEof(Entry entry, 
    		IoProcessor processor, Object msg) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelReadEof(nextFilter, processor, msg);
    }
    
    public void fireFilterWrite(IoBuffer buf) throws Exception {
        Entry head = this.tail;
        callNextFilterWrite(head, processor, buf);
    }

    private void callNextFilterWrite(Entry entry, 
    		IoProcessor processor, IoBuffer buf) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.filterWrite(nextFilter, processor, buf);
    }
    
    public void fireChannelSend(WriteRequest writeRequest) throws Exception {
        Entry head = this.head;
        callNextFilterChannelSend(head, processor, writeRequest);
    }

    private void callNextFilterChannelSend(Entry entry, 
		IoProcessor processor, WriteRequest writeRequest) throws Exception {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelSend(nextFilter, processor, writeRequest);
    }
    
    public void fireChannelClose() {
        Entry head = this.head;
        callNextFilterChannelClose(head, processor);
    }

    private void callNextFilterChannelClose(Entry entry, IoProcessor processor) {
        IoFilter filter = entry.getFilter();
        NextFilter nextFilter = entry.getNextFilter();
        filter.channelClose(nextFilter, processor);
    }

    /**
     * 会话IO异常发生时触发
     */
    public void fireExceptionCaught(Throwable cause) {
        Entry head = this.head;
        callNextExceptionCaught(head, processor, cause);
    }

    private void callNextExceptionCaught(Entry entry, IoProcessor processor, Throwable cause) {
		try {
		    IoFilter filter = entry.getFilter();
		    NextFilter nextFilter = entry.getNextFilter();
		    filter.exceptionCaught(nextFilter, processor, cause);
		} catch (Throwable t) {
		    Logger.warn(t, "Unexpected exception from exceptionCaught handler.");
		}
    }
    
    private void checkAddable(IoFilter filter) {
    	String name = filter.getName();
        if (entryMap.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Other filter is using the same name '" + name + "'");
        }
    }
    
    private Entry checkRemovable(String baseName) {
    	Entry e = entryMap.get(baseName);
        if (e == null) {
            throw new IllegalArgumentException("Filter not found:" + baseName);
        }
        return e;
    }
    
    private void register(IoFilter filter) {
        Entry newEntry = new Entry(filter);
        newEntry.prevEntry = tail.prevEntry;
        tail.prevEntry.nextEntry = newEntry;
        newEntry.nextEntry = tail;
        tail.prevEntry = newEntry;
        entryMap.put(newEntry.getName(), newEntry);
    }
    
    private void deregister(Entry entry) {
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        prevEntry.nextEntry = nextEntry;
        nextEntry.prevEntry = prevEntry;
        entryMap.remove(entry.getName());
        entry.getFilter().destroy();
    }
	
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{ ");

        boolean empty = true;

        Entry e = head.nextEntry;
        while (e != tail) {
            if (!empty) {
                buf.append(", ");
            } else {
                empty = false;
            }

            buf.append('(');
            buf.append(e.getName());
            buf.append(':');
            buf.append(e.getFilter());
            buf.append(')');

            e = e.nextEntry;
        }

        if (empty) {
            buf.append("empty");
        }

        buf.append(" }");

        return buf.toString();
    }
    
    /**
     * 责任链头节点
     */
    private final class HeadFilter extends IoFilterAdaptor {
    	private static final String NAME = "HeadFilter";
    	
		@Override
		public final String getName() {
			return NAME;
		}

		@Override
		public final void filterWrite(NextFilter nextFilter, IoProcessor processor,
				IoBuffer buffer) throws Exception {
			final WriteRequest writeRequest = processor.getWriteRequest();
			writeRequest.setLastWriteRequest(buffer);
			writeRequest.offer(buffer);
		}
    }

    /**
     * 责任链尾节点
     */
    private final class TailFilter extends IoFilterAdaptor {
    	private static final String NAME = "TailFilter";
    	
		@Override
		public final String getName() {
			return NAME;
		}

		@Override
		public final void channelAccept(NextFilter nextFilter, IoProcessor processor,
				EventChannel channel) throws Exception {
			processor.channelAccept(channel);
		}

		@Override
		public final void channelConnect(NextFilter nextFilter, IoProcessor processor)
				throws Exception {
			final IoEvent event = processor.getEvent();
			event.unInterestEvent(IoEvent.OP_CONNECT).interestEvent(IoEvent.OP_READ);
			processor.channelConnect();
		}

		@Override
		public final void channelRead(NextFilter nextFilter, IoProcessor processor,
				Object message) throws Exception {
			processor.setActionTime(System.currentTimeMillis());
			processor.channelRead(message);
		}

		@Override
		public void channelReadEof(NextFilter nextFilter,
				IoProcessor processor, Object message) throws Exception {
			processor.channelReadEof(message);
		}

		@Override
		public final void channelSend(NextFilter nextFilter, IoProcessor processor,
				WriteRequest writeRequest) throws Exception {
			processor.setActionTime(System.currentTimeMillis());
			// 数据发送完毕，注册读事件，注销写事件，不然会不断触发Selector写事件进行空数据发送
			final IoEvent event = processor.getEvent();
			event.unInterestEvent(IoEvent.OP_WRITE).interestEvent(IoEvent.OP_READ);

			writeRequest.clear();
			processor.channelSend(writeRequest);
			processor.clear();
		}

		@Override
		public final void filterWrite(NextFilter nextFilter, IoProcessor processor,
				IoBuffer buf) throws Exception {
			nextFilter.filterWrite(processor, buf);
		}

		@Override
		public final void channelClose(NextFilter nextFilter, IoProcessor processor) {
			processor.getWriteRequest().clear();
			processor.channelClose();
		}

		@Override
		public final void exceptionCaught(NextFilter nextFilter,
				IoProcessor processor, Throwable cause) {
			processor.channelError(cause);
		}
    }
    
	/**
     * {@link IoFilterChain}元素节点，可理解为{@link IoFilter}结点
     */
    public class Entry {
    	private Entry prevEntry;

        private Entry nextEntry;

        private IoFilter filter;

        private final NextFilter nextFilter;
        
        private Entry(IoFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter");
            }

            this.filter = filter;
            this.nextFilter = new NextFilter() {
            	@Override
				public void channelAccept(IoProcessor processor, EventChannel channel) throws Exception {
            		Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelAccept(nextEntry, processor, channel);
				}

				@Override
				public void channelConnect(IoProcessor processor) throws Exception {
					Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelConnect(nextEntry, processor);
				}

				@Override
				public void channelRead(IoProcessor processor, Object message) throws Exception {
					Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelRead(nextEntry, processor, message);
				}
				
				@Override
				public void channelReadEof(IoProcessor processor, Object message) throws Exception {
					Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelReadEof(nextEntry, processor, message);
				}

				@Override
				public void channelSend(IoProcessor processor, 
						WriteRequest writeRequest) throws Exception {
					Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelSend(nextEntry, processor, writeRequest);
				}

				@Override
				public void filterWrite(IoProcessor processor, IoBuffer buffer) throws Exception {
					Entry nextEntry = Entry.this.prevEntry;
					callNextFilterWrite(nextEntry, processor, buffer);
				}

				@Override
                public void channelClose(IoProcessor processor) {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextFilterChannelClose(nextEntry, processor);
                }
            	
				@Override
                public void exceptionCaught(IoProcessor processor, Throwable cause) {
                    Entry nextEntry = Entry.this.nextEntry;
                    callNextExceptionCaught(nextEntry, processor, cause);
                }

				@Override
            	public String toString() {
                    return Entry.this.nextEntry.getName();
                }
            };
        }
        
        public String getName() {
        	return filter.getName();
        }
    	
        public IoFilter getFilter() {
        	return filter;
        }

        public NextFilter getNextFilter() {
        	return nextFilter;
        }
        
        public void add(IoFilter filter) {
        	IoFilterChain.this.add(filter);
        }

        public void remove() {
        	IoFilterChain.this.remove(this.getName());
        }
        
        @Override
        public String toString() {
            return getName();
        }
    }
}
