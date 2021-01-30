package cloud.apposs.netkit.rxio;

import cloud.apposs.util.Errno;
import cloud.apposs.util.StandardResult;

/**
 * 当响应式中有错误结果或者异常时的回滚操作
 */
public class OnSubscribeRollback<T> implements RxIo.OnSubscribe<StandardResult> {
    private final RxIo<T> source;

    final IoFunction<StandardResult, StandardResult> rollback;

    public OnSubscribeRollback(RxIo<T> source, IoFunction<StandardResult, StandardResult> rollback) {
        this.source = source;
        this.rollback = rollback;
    }

    @Override
    public void call(SafeIoSubscriber<? super StandardResult> subscriber) throws Exception {
        RollbackSubscriber<T> parent = new RollbackSubscriber<T>(subscriber, rollback);
        source.subscribe(parent).start();
    }

    static final class RollbackSubscriber<T> implements IoSubscriber<T> {
        final IoSubscriber<? super StandardResult> actual;

        final IoFunction<StandardResult, StandardResult> rollback;

        public RollbackSubscriber(IoSubscriber<? super StandardResult> actual,
                                  IoFunction<StandardResult, StandardResult> rollback) {
            this.actual = actual;
            this.rollback = rollback;
        }

        @Override
        public void onNext(T value) throws Exception {
            if (value instanceof StandardResult) {
                StandardResult result = (StandardResult) value;
                if (result.isError()) {
                    // 将处理结果为失败时调用回滚操作
                    actual.onNext(rollback.call(result));
                } else {
                    actual.onNext(result);
                }
            } else {
                // 非StandardResult则进行包装再传递
                actual.onNext(StandardResult.success(value));
            }
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable cause) {
            try {
                // 异常也作为错误结果处理
                StandardResult result = StandardResult.error(Errno.ERROR, cause);
                actual.onNext(rollback.call(result));
            } catch (Exception e) {
                actual.onError(e);
            }
        }
    }
}
