package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

public final class OnSubscribeIo<T> implements OnSubscribe<T> {
    private final EventLoopGroup group;

    private final IoProcessor processor;

    public OnSubscribeIo(EventLoopGroup g, IoProcessor p) {
        this.group = g;
        this.processor = p;
    }

    @Override
    public void call(SafeIoSubscriber<? super T> t) throws Exception {
        // 链式调用已经到最底层，交由EventLoopGroup请求产生数据
        IoProcessorSubscriber<T> subscriber = new IoProcessorSubscriber<T>(t);
        processor.setContext(subscriber);
        group.addToLoop(processor);
    }

    final class IoProcessorSubscriber<T> implements IoSubscriber<T> {
        private final IoSubscriber<? super T> actual;

        public IoProcessorSubscriber(IoSubscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onNext(T value) {
            try {
                actual.onNext(value);
                actual.onCompleted();
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable t) {
            try {
                actual.onError(t);
            } finally {
                actual.onCompleted();
            }
        }
    }
}
