package cloud.apposs.netkit.rxio;

/**
 * 基于数组的数据发送器
 */
public class OnSubscribeFromArray<T> implements RxIo.OnSubscribe<T> {
	private final T[] array;

	public OnSubscribeFromArray(T[] array) {
        this.array = array;
    }

	@Override
	public void call(SafeIoSubscriber<? super T> t) throws Exception {
	    try {
            for (int i = 0; i < array.length; i++) {
                t.onNext(array[i]);
            }
	    } finally{
	        t.onCompleted();
        }
	}
}
