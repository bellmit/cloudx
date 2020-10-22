package cloud.apposs.netkit.rxio;

/**
 * 异步请求数据后的订阅消费者，负责将请求到的数据进行逻辑处理并发送，由业务方实现业务处理逻辑
 */
public interface IoSubscriber<T> {
	void onNext(T value) throws Exception;
	
	/**
	 * 任务结束时 回调，
	 * 注意如果不同的IoSubcriber实现类在{@link #onNext(Object)}、{@link #onError(Throwable)}内实现{actual.onCompleted()}时，
	 * 那{@link #onCompleted()}则不需要实现，避免上层主动调用{@link #onCompleted()}导致该方法的主要逻辑失效
	 */
	void onCompleted();

	/**
	 * 任务处理链中出现的异常均由此类捕捉并处理
	 */
	void onError(Throwable cause);
}
