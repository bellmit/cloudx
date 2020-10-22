package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

/**
 * 重复执行流，底层采用for循环来执行，
 * 注意在RxIo.XXX.repeat，之前的所有流方法都会重复执行N次
 */
public class OnSubscribeRepeat<T> implements OnSubscribe<T> {
    private final RxIo<T> source;

    /**
     * 重复执行次数
     */
    private final int count;

    public OnSubscribeRepeat(RxIo<T> source, int count) {
        this.source = source;
        this.count = count;
    }

    @Override
    public void call(SafeIoSubscriber<? super T> t) throws Exception {
        RepeatSubscriber<T> subscriber = new RepeatSubscriber<T>(t);
        source.subscribe(subscriber).start();
    }

    class RepeatSubscriber<T> implements IoSubscriber<T> {
        private final IoSubscriber<? super T> actual;

        RepeatSubscriber(IoSubscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onNext(T value) throws Exception {
            for (int i = 0; i < count; i++) {
                actual.onNext(value);
                if (i == count - 1) {
                    actual.onCompleted();
                }
            }
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable cause) {
            try {
                actual.onError(cause);
            } finally {
                actual.onCompleted();
            }
        }
    }
}
