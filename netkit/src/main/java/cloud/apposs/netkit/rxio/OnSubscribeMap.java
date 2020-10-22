package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

public final class OnSubscribeMap<T, R> implements OnSubscribe<R> {
	private final RxIo<T> source;
	
	private final IoFunction<? super T, ? extends R> transformer;
	
	public OnSubscribeMap(RxIo<T> source, IoFunction<? super T, ? extends R> transformer) {
        this.source = source;
        this.transformer = transformer;
    }
	
	@Override
	public void call(final SafeIoSubscriber<? super R> t) throws Exception {
		MapSubscriber<T, R> parent = new MapSubscriber<T, R>(t, transformer);
        source.subscribe(parent).start();
	}
	
	static final class MapSubscriber<T, R> implements IoSubscriber<T> {
        private final IoSubscriber<? super R> actual;

        private final IoFunction<? super T, ? extends R> mapper;

        public MapSubscriber(IoSubscriber<? super R> actual, IoFunction<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) throws Exception {
			R result = mapper.call(t);
			actual.onNext(result);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }
    }
}
