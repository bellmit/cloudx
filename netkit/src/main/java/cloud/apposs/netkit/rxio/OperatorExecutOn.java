package cloud.apposs.netkit.rxio;

import java.util.concurrent.Executor;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

public class OperatorExecutOn<T> implements OnSubscribe<T> {
	private final OnSubscribe<T> parent;
	
	private final Executor executor;
	
	public OperatorExecutOn(OnSubscribe<T> parent, Executor executor) {
		this.parent = parent;
		this.executor = executor;
	}
	
	@Override
	public void call(final SafeIoSubscriber<? super T> t) throws Exception {
		IoSubscriber<? super T> is = new ExecutorOnSubscriber<T>(t, executor);
		parent.call(new SafeIoSubscriber<T>(is));
	}
	
	static final class ExecutorOnSubscriber<T> implements IoSubscriber<T> {
		private final IoSubscriber<? super T> child;
		
		private final Executor executor;
		
		public ExecutorOnSubscriber(IoSubscriber<? super T> child, Executor executor) {
			this.child = child;
			this.executor = executor;
		}
		
		@Override
		public void onNext(final T value) throws Exception {
			executor.execute(new Runnable() {
    			@Override
    			public void run() {
    				try {
						child.onNext(value);
					} catch (Exception e) {
						child.onError(e);
					}
    			}
    		});
		}

		@Override
		public void onCompleted() {
			executor.execute(new Runnable() {
    			@Override
    			public void run() {
					child.onCompleted();
    			}
    		});
		}

		@Override
		public void onError(final Throwable t) {
			executor.execute(new Runnable() {
    			@Override
    			public void run() {
					child.onError(t);
    			}
    		});
		}
	}
}
