package cloud.apposs.netkit.rxio;

/**
 * Io数据发射器，由业务方实现数据的生成，有返回值
 */
public interface IoEmitter<R> {
	R call() throws Exception;
}
