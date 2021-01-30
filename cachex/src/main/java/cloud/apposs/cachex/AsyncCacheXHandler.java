package cloud.apposs.cachex;

/**
 * CacheX异步回调接口
 */
public interface AsyncCacheXHandler<V> {
	/**
	 * CacheX异步回调执行完成后的回调
	 * 
	 * @param result 异步执行结果
	 * @param cause  异步执行如果产生异常则不为空
	 */
	void operationComplete(V result, Throwable cause);
}
