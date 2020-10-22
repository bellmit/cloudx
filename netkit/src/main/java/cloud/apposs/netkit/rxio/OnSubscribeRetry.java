package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

/**
 * RxIo执行异常时重试机制，主要应用于是网络请求异常时的重试逻辑
 */
public class OnSubscribeRetry<T> implements OnSubscribe<T> {
    private final RxIo<T> source;

    private final IoFunction<Throwable, ? extends RxIo<T>> handler;

    public OnSubscribeRetry(RxIo<T> source, IoFunction<Throwable, ? extends RxIo<T>> handler) {
        this.source = source;
        this.handler = handler;
    }

    @Override
    public void call(SafeIoSubscriber<? super T> t) throws Exception {
        RetrySubscriber<T> parent = new RetrySubscriber<T>(t, handler);
        source.subscribe(parent).start();
    }

    static class RetrySubscriber<T> implements IoSubscriber<T> {
        private final IoSubscriber<? super T> actual;

        private final IoFunction<Throwable, ? extends RxIo<T>> handler;

        RetrySubscriber(IoSubscriber<? super T> actual, IoFunction<Throwable, ? extends RxIo<T>> handler) {
            this.actual = actual;
            this.handler = handler;
        }

        @Override
        public void onNext(T value) throws Exception {
            actual.onNext(value);
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable cause) {
            try {
                RxIo<T> source = handler.call(cause);
                if (source == null) {
                    actual.onError(cause);
                } else {
                    source.subscribe(this).start();
                }
            } catch (Throwable e) {
                actual.onError(e);
            }
        }
    }
}
