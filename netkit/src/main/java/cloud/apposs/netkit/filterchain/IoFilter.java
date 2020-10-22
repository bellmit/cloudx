package cloud.apposs.netkit.filterchain;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;

/**
 * {@link IoProcessor}过滤器，
 * 注意所有过滤器在所有{@link IoProcessor}是属于单例，
 * 如果需要保存会话级状态属性需要附属到{@link IoProcessor#setAttribute}中，
 * 规则：
 * <pre>
 * 1、每个过滤器存储的内存分配均要采用{@link cloud.apposs.netkit.buffer.IoAllocator#allocate(int)}来分配内存以便于灵活配置是否采用零拷贝来接收/发送网络数据
 * 2、在{@link IoFilter#channelSend(NextFilter, IoProcessor, WriteRequest)}发送完数据时reset重置该类之前分配内存以便于重复利用内存
 * 3、在{@link IoFilter#channelClose(NextFilter, IoProcessor)}关闭会话时release释放该类之前分配的内存资源
 * </pre>
 */
public interface IoFilter {
	/**
	 * 返回过滤器名称
	 */
	String getName();
	
	/**
	 * 初始化，只在服务启动的时候初始化
	 */
	void init();
	
	/**
	 * 销毁
	 */
	void destroy();
	
	/**
     * 发生IO异常时触发
     */
    void exceptionCaught(NextFilter nextFilter, IoProcessor processor, Throwable cause);
	
	void channelAccept(NextFilter nextFilter, IoProcessor processor, EventChannel channel) throws Exception;
	
	void channelConnect(NextFilter nextFilter, IoProcessor processor) throws Exception;
	
	void channelRead(NextFilter nextFilter, IoProcessor processor, Object message) throws Exception;
	
	void channelReadEof(NextFilter nextFilter, IoProcessor processor, Object message) throws Exception;
	
	/**
	 * 数据发送完毕时的回调，一般用于会话重置
	 */
	void channelSend(NextFilter nextFilter, IoProcessor processor, WriteRequest writeRequest) throws Exception;
	
	/**
	 * 对发送的数据进行再编码
	 */
	void filterWrite(NextFilter nextFilter, IoProcessor processor, IoBuffer buffer) throws Exception;
	
	/**
	 * 会话关闭时的回调(包括正常会话关闭和各种异常时导致的会话关闭)，一般用于资源释放
	 */
    void channelClose(NextFilter nextFilter, IoProcessor processor);
    
	/**
	 * {@link IoFilterChain}责任链中的下一个过滤器{@link IoFilter}
	 */
	public interface NextFilter {
		void channelAccept(IoProcessor processor, EventChannel channel) throws Exception;
		
		void channelConnect(IoProcessor processor) throws Exception;
		
		void channelRead(IoProcessor processor, Object message) throws Exception;
		
		void channelReadEof(IoProcessor processor, Object message) throws Exception;
		
		void channelSend(IoProcessor processor, WriteRequest writeRequest) throws Exception;
		
		void filterWrite(IoProcessor processor, IoBuffer buffer) throws Exception;
		
        void channelClose(IoProcessor processor);
        
        void exceptionCaught(IoProcessor processor, Throwable cause);
	}
}
