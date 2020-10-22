package cloud.apposs.netkit.client;

/**
 * 业务逻辑处理接口，全局单例
 */
public interface ClientHandler {
	void channelConnect(ClientHandlerContext context) throws Exception;
	
	void channelRead(ClientHandlerContext context, Object message) throws Exception;

	void channelError(Throwable cause);
}
