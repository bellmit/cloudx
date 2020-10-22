package cloud.apposs.netkit.rxio;

import java.util.Iterator;

/**
 * 基于迭代器的数据发送器
 */
public class OnSubscribeFromIterable<T> implements RxIo.OnSubscribe<T> {
    private final Iterable<? extends T> iterable;

    public OnSubscribeFromIterable(Iterable<? extends T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public void call(SafeIoSubscriber<? super T> t) throws Exception {
        try {
            Iterator<? extends T> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                T value = iterator.next();
                t.onNext(value);
            }
        } finally{
            t.onCompleted();
        }
    }
}
