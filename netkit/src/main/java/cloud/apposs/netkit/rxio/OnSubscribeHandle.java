package cloud.apposs.netkit.rxio;

import cloud.apposs.util.StandardResult;

public class OnSubscribeHandle<T> implements RxIo.OnSubscribe<StandardResult> {
    private final RxIo<T> source;

    final IoFunction<? super T, StandardResult> predicate;

    public OnSubscribeHandle(RxIo<T> source, IoFunction<? super T, StandardResult> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public void call(SafeIoSubscriber<? super StandardResult> subscriber) throws Exception {
        StandardResultSubscriber<T> parent = new StandardResultSubscriber<T>(subscriber, predicate);
        source.subscribe(parent).start();
    }

    public static final class StandardResultException extends Exception {
        private static final long serialVersionUID = -6099542992767550343L;

        private final StandardResult result;

        public StandardResultException(StandardResult result) {
            this.result = result;
        }

        public StandardResultException(String msg, StandardResult result) {
            super(msg);
            this.result = result;
        }

        public StandardResult getResult() {
            return result;
        }
    }

    static final class StandardResultSubscriber<T> implements IoSubscriber<T> {
        final IoSubscriber<? super StandardResult> actual;

        final IoFunction<? super T, StandardResult> predicate;

        public StandardResultSubscriber(IoSubscriber<? super StandardResult> actual,
                IoFunction<? super T, StandardResult> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) throws Exception {
            StandardResult result = predicate.call(t);
            if (result.isSuccess()) {
                actual.onNext(result);
            } else {
                actual.onError(new StandardResultException(result));
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
