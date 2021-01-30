package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

import java.util.Iterator;

/**
 * 需要进行前置条件判断的多个RxIo串行执行，
 * 下一个串行执行的RxIo流会根据上一个执行的任务来判断是否执行，如果上一个流执行不符合结果则解决False失败处理，
 * 主要业务有：
 * 1. 订单下单，多个子系统串行执行，依赖上一任务的执行结果来决定是否执行下一任务
 * 2. 多子任务定时执行，下一个任务依赖上一个任务的执行结果
 */
public class OperateorFlat<T> implements OnSubscribe<Boolean> {
    private final Iterable<? extends RxIo<? extends T>> sequences;

    /**
     * 具体执行是否继续执行根据此方法进行判断
     */
    private final IoFunction<? super T, Boolean> predicate;

    public OperateorFlat(Iterable<? extends RxIo<? extends T>> sequences, IoFunction<? super T, Boolean> predicate) {
        if (sequences == null || !sequences.iterator().hasNext()) {
            throw new IllegalArgumentException("sequences");
        }
        this.sequences = sequences;
        this.predicate = predicate;
    }

    @Override
    public void call(SafeIoSubscriber<? super Boolean> t) throws Exception {
        Iterator<? extends RxIo<? extends T>> iterator = sequences.iterator();
        FlatSubscriber<T> subscriber = new FlatSubscriber<T>(t, iterator, predicate);
        RxIo<? extends T> source = iterator.next();
        source.subscribe(subscriber).start();
    }

    private static class FlatSubscriber<T> implements IoSubscriber<T> {
        final IoSubscriber<? super Boolean> actual;

        final Iterator<? extends RxIo<? extends T>> sequences;

        final IoFunction<? super T, Boolean> predicate;

        public FlatSubscriber(IoSubscriber<? super Boolean> actual, Iterator<? extends RxIo<? extends T>> sequences, IoFunction<? super T, Boolean> predicate) {
            this.actual = actual;
            this.sequences = sequences;
            this.predicate = predicate;
        }

        @Override
        public void onNext(T value) throws Exception {
            boolean success = predicate.call(value);
            if (success) {
                // 该次请求验证成功，获取下一个请求继续执行
                if (sequences.hasNext()) {
                    RxIo<? extends T> source = sequences.next();
                    source.subscribe(this).start();
                } else {
                    try {
                        // 已经没有下一个请求了，直接调用业务实现接口返回
                        actual.onNext(true);
                    } finally {
                        actual.onCompleted();
                    }
                }
            } else {
                try {
                    // 该次请求验证失败，直接调用业务实现接口返回
                    actual.onNext(false);
                } finally {
                    actual.onCompleted();
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            try {
                actual.onError(e);
            } finally {
                actual.onCompleted();
            }
        }

        /**
         * 有可能外层会调用onComplete方法，先直接屏蔽，让组件自己触发onComplete逻辑
         */
        @Override
        public void onCompleted() {
        }
    }
}
