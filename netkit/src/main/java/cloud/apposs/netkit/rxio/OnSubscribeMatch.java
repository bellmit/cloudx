package cloud.apposs.netkit.rxio;

import cloud.apposs.util.Errno;

public final class OnSubscribeMatch<T> implements RxIo.OnSubscribe<T> {
	private final RxIo<T> source;

	final IoFunction<? super T, Errno> predicate;

    public OnSubscribeMatch(RxIo<T> source, IoFunction<? super T, Errno> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
	
	@Override
	public void call(SafeIoSubscriber<? super T> t) throws Exception {
		MatchSubscriber<T> parent = new MatchSubscriber<T>(t, predicate);
		source.subscribe(parent).start();
	}
	
	static final class MatchException extends Exception {
		private static final long serialVersionUID = -6099544492768850343L;

		private Errno errno;
		
		public MatchException(Errno errno) {
			this.errno = errno;
		}
		
		public MatchException(String msg, Errno errno) {
			super(msg);
			this.errno = errno;
		}
		
		public Errno errno() {
			return errno;
		}
	}
	
	static final class MatchSubscriber<T> implements IoSubscriber<T> {
        final IoSubscriber<? super T> actual;

        final IoFunction<? super T, Errno> predicate;

        public MatchSubscriber(IoSubscriber<? super T> actual, IoFunction<? super T, Errno> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) throws Exception {
            Errno result = predicate.call(t);
            if (result == Errno.OK) {
            	actual.onNext(t);
            } else {
            	actual.onError(new MatchException(result));
            }
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
