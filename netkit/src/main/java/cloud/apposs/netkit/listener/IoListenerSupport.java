package cloud.apposs.netkit.listener;

import cloud.apposs.netkit.IoProcessor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IoListenerSupport {
	private final List<IoListener> listeners = new CopyOnWriteArrayList<IoListener>();
	
	public void add(IoListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
	
	public void remove(IoListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
	
	public void fireChannelAccept(final IoProcessor processor) {
        for (IoListener listener : listeners) {
        	listener.channelAccept(processor);
        }
    }
	
	public void fireChannelConnect(final IoProcessor processor) {
        for (IoListener listener : listeners) {
        	listener.channelConnect(processor);
        }
	}
	
	public void fireChannelRead(final IoProcessor processor, long readBytesLen) {
        for (IoListener listener : listeners) {
        	listener.channelRead(processor, readBytesLen);
        }
	}
	
	public void fireChannelSend(final IoProcessor processor, long sendBytesLen) {
        for (IoListener listener : listeners) {
        	listener.channelSend(processor, sendBytesLen);
        }
	}
	
	public void fireChannelClose(final IoProcessor processor) {
        for (IoListener listener : listeners) {
        	listener.channelClose(processor);
        }
    }

	public void fireChannelError(final IoProcessor processor, Throwable t) {
		try {
			for (IoListener listener : listeners) {
	        	listener.channelError(processor, t);
	        }
		} catch(Throwable tt) {
		}
	}
}
