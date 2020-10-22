package cloud.apposs.netkit.filterchain.executor;

import java.util.EventListener;

import cloud.apposs.netkit.filterchain.executor.ThreadPool.Worker;

public interface ThreadPoolListener extends EventListener {
	void workerBusy();
	
	void workerDead(Worker worker);
	
	void workerTimeout(Worker worker);
}
