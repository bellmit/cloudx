package cloud.apposs.netkit.rxio;

public final class IoActionSubscriber<T> implements IoSubscriber<T> {
	public static final IoAction<Void> EMPTY_ACTION = new EmptyAction();
	public static final IoAction<Throwable> UNSUPPORTED_ACTION = new UnSupportedAction();
	
    private final IoAction<? super T> onNext;
    private final IoAction<Throwable> onError;
    private final IoAction<Void> onCompleted;

    public IoActionSubscriber(IoAction<? super T> onNext, 
    		IoAction<Throwable> onError, IoAction<Void> onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(T t) throws Exception {
        onNext.call(t);
    }

    @Override
    public void onError(Throwable t) {
        try {
			onError.call(t);
		} catch (Throwable e) {
		}
    }

    @Override
    public void onCompleted() {
        try {
			onCompleted.call(null);
		} catch (Throwable t) {
		}
    }
    
    static final class EmptyAction implements IoAction<Void> {
		@Override
		public void call(Void t) throws Exception {
		}
    }
    
    static final class UnSupportedAction implements IoAction<Throwable> {
		@Override
		public void call(Throwable t) throws Exception {
			throw new UnsupportedOperationException();
		}
    }
}
