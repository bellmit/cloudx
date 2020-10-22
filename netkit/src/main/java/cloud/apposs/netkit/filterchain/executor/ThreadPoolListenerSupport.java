package cloud.apposs.netkit.filterchain.executor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cloud.apposs.netkit.filterchain.executor.ThreadPool.Worker;

public class ThreadPoolListenerSupport {
	private final List<ThreadPoolListener> listeners = new CopyOnWriteArrayList<ThreadPoolListener>();
	
	public void add(ThreadPoolListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
	
	public void remove(ThreadPoolListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
	
	public void fireWorkerBusy() {
        for (ThreadPoolListener listener : listeners) {
        	listener.workerBusy();
        }
    }

	public void fireWorkerDead(Worker worker) {
		for (ThreadPoolListener listener : listeners) {
        	listener.workerDead(worker);
        }
	}

	public void fireWorkerTimeout(Worker worker) {
		for (ThreadPoolListener listener : listeners) {
        	listener.workerTimeout(worker);
        }
	}
}
