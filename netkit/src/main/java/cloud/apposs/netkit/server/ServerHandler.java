package cloud.apposs.netkit.server;

import cloud.apposs.netkit.WriteRequest;

/**
 * 服务器端的业务处理器，由各业务实现具体自身的业务逻辑
 * 注意:
 * 1、该Handler在所有请求中都是单例
 * 2、该业务处理不能阻塞请求，否则会导致性能下降，如果方法耗时比较久建议改成线程池的方式实现
 */
public interface ServerHandler {
	byte[] getWelcome();
	
	void channelAccept(ServerHandlerContext context) throws Exception;
	
	void channelClose(ServerHandlerContext context);
	
	void channelRead(ServerHandlerContext context, Object message) throws Exception;
	
	void channelSend(ServerHandlerContext context, WriteRequest request);
	
	void channelError(ServerHandlerContext context, Throwable cause);

	/**
	 * 解析{@link ServerHandler}自身注释，并实现各自的逻辑处理
	 */
	void parseAnnotation(IoServer server, Class<? extends ServerHandler> clazz);

	/**
	 * 关闭系统服务
	 */
	void destroy();
}
