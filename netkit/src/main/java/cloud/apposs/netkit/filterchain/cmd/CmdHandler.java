package cloud.apposs.netkit.filterchain.cmd;

import cloud.apposs.netkit.IoProcessor;

public interface CmdHandler<T> {
	/**
	 * 解析接收数据中的CMD指令
	 */
	T parseCmd(Object message);

	/**
	 * 判断是否已经开始只读服务，
	 * 如果已经开启则接口层直接调用{@link #discard(IoProcessor, Object)}}丢弃请求
	 */
	boolean readonly(T cmd);

	/**
	 * 丢弃请求的业务处理
	 * 
	 * 丢弃请求时还需要发送数据返回true
	 */
	boolean discard(IoProcessor processor, Object message) throws Exception;
}
