package cloud.apposs.netkit.rxio;

/**
 * Io数据变换器，由业务方调用实现对数据的转换，有返回值
 */
public interface IoFunction<T, R> {
	R call(T t) throws Exception;
}
