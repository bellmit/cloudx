package cloud.apposs.netkit.rxio;

/**
 * {@link IoSubscriber}包装类，保存当次响应会话内部状态，只有系统内部内调用避免暴露太多属性给业务方
 */
public class SafeIoSubscriber<T> implements IoSubscriber<T> {
	protected final IoSubscriber<? super T> actual;
	
	public SafeIoSubscriber(IoSubscriber<? super T> actual) {
        this.actual = actual;
    }
	
	@Override
	public void onCompleted() {
		actual.onCompleted();
	}

	@Override
	public void onError(Throwable e) {
		actual.onError(e);
	}

	@Override
	public void onNext(T value) throws Exception {
		actual.onNext(value);
	}
}
