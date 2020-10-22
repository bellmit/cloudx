package cloud.apposs.netkit.rxio;

/**
 * Io数据变换器，主要由系统内OnSubscribe各实现类实现，无返回值
 */
public interface IoAction<T> {
	void call(T t) throws Exception;
}
