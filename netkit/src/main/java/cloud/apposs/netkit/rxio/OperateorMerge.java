package cloud.apposs.netkit.rxio;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

/**
 * 合并多个请求结果，
 * 一般用于同时请求多个网络连接场景，
 * 无论成功或者失败当所有请求都结束后最后都会调用{@link IoSubscriber#onCompleted()}方法
 */
public class OperateorMerge<T> implements OnSubscribe<T> {
	private final RxIo<? extends T>[] sequences;
	
	public OperateorMerge(RxIo<? extends T>[] sequences) {
		if (sequences == null || sequences.length <= 0) {
			throw new IllegalArgumentException("sequences");
		}
		this.sequences = sequences;
	}
	
	@SuppressWarnings("unchecked")
	public OperateorMerge(List<? extends RxIo<? extends T>> sequences) {
		if (sequences == null || sequences.size() <= 0) {
			throw new IllegalArgumentException("sequences");
		}
		this.sequences = new RxIo[sequences.size()];
		sequences.toArray(this.sequences);
	}

	@Override
	public void call(SafeIoSubscriber<? super T> t) throws Exception {
		MergeSubscriber<T> subscriber = new MergeSubscriber<T>(t, sequences.length);
		for (int i = 0; i < sequences.length; i++) {
        	RxIo<? extends T> rxio = sequences[i];
            rxio.subscribe(subscriber).start();
        }
	}
	
	static final class MergeSubscriber<T> implements IoSubscriber<T> {
		private final IoSubscriber<? super T> actual;
		
		private final int total;
		
		private final AtomicInteger index;
		
        public MergeSubscriber(IoSubscriber<? super T> actual, int total) {
            this.actual = actual;
            this.total = total;
            this.index = new AtomicInteger(0);
        }
		
		@Override
		public void onNext(T value) {
			try {
				actual.onNext(value);
				index.incrementAndGet();
			} catch(Throwable t) {
				onError(t);
			} finally {
				if (index.get() >= total) {
					actual.onCompleted();
				}
			}
		}
		
		@Override
		public void onError(Throwable t) {
			try {
				index.incrementAndGet();
				actual.onError(t);
			} finally {
				if (index.get() >= total) {
					actual.onCompleted();
				}
			}
		}
		
		/**
		 * 有可能外层会调用onComplete方法，
		 * 所以Merge自己直接屏蔽，让Merge自己触发onComplete逻辑
		 */
		@Override
		public void onCompleted() {
		}
	}
}
