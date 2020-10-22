package cloud.apposs.netkit.rxio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

/**
 * 合并多个请求结果，
 * 一般用于同时请求多个网络连接场景，
 * 无论成功或者失败当所有请求都结束后最后才会合并所有结果并只调用一次{@link IoSubscriber#onNext(Object)}方法
 */
public class OperateorMergeList<T> implements OnSubscribe<List<T>> {
	private final RxIo<? extends T>[] sequences;
	
	private final boolean skipError;
	
	public OperateorMergeList(RxIo<? extends T>[] sequences) {
		this(sequences, true);
	}
	
	public OperateorMergeList(RxIo<? extends T>[] sequences, boolean skipError) {
		if (sequences == null || sequences.length <= 0) {
			throw new IllegalArgumentException("sequences");
		}
		this.sequences = sequences;
		this.skipError = skipError;
	}
	
	@Override
	public void call(SafeIoSubscriber<? super List<T>> t) throws Exception {
		MergeListSubscriber<T> subscriber = new MergeListSubscriber<T>(t, sequences.length, skipError);
		for (int i = 0; i < sequences.length; i++) {
        	RxIo<? extends T> rxio = sequences[i];
            rxio.subscribe(subscriber).start();
        }		
	}

	static final class MergeListSubscriber<T> implements IoSubscriber<T> {
		private final IoSubscriber<? super List<T>> actual;
		
		private final int total;
		
		private final AtomicInteger index;
		
		private final List<T> valueList;
		
		private final boolean skipError;

        public MergeListSubscriber(SafeIoSubscriber<? super List<T>> t, int total, boolean skipError) {
        	this.actual = t;
            this.total = total;
            this.index = new AtomicInteger(0);
            this.valueList = new ArrayList<T>(total);
            this.skipError = skipError;
        }

		@Override
		public void onNext(T value) throws Exception {
			try {
				index.incrementAndGet();
				valueList.add(value);
				if (index.get() >= total) {
					actual.onNext(valueList);
					actual.onCompleted();
				}
			} catch(Throwable t) {
				actual.onError(t);
			}
		}
        
		@Override
		public void onCompleted() {
			index.incrementAndGet();
			if (index.get() >= total) {
				actual.onCompleted();
			}
		}

		@Override
		public void onError(Throwable t) {
			try {
				index.incrementAndGet();
				if (index.get() >= total) {
					actual.onNext(valueList);
					actual.onCompleted();
				}
				if (!skipError) {
					actual.onError(t);
				}
			} catch (Throwable e) {
				actual.onError(t);
			}
		}
	}
}
