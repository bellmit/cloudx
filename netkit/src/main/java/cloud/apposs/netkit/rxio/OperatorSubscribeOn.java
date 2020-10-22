package cloud.apposs.netkit.rxio;

import java.util.concurrent.Executor;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

public class OperatorSubscribeOn<T> implements OnSubscribe<T> {
	private final RxIo<T> source;
	
	private final Executor executor;
	
	public OperatorSubscribeOn(RxIo<T> source, Executor executor) {
		this.source = source;
		this.executor = executor;
	}
	
	@Override
	public void call(final SafeIoSubscriber<? super T> t) throws Exception {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				source.subscribe(t).start();
			}
		});
	}
}
