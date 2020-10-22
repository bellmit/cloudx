package cloud.apposs.netkit.listener;

import cloud.apposs.netkit.IoProcessor;

import java.util.EventListener;

/**
 * 网络监听器
 */
public interface IoListener extends EventListener {
	void channelAccept(IoProcessor processor);
	
    void channelConnect(IoProcessor processor);
    
    void channelRead(IoProcessor processor, long readBytesLen);
    
    void channelSend(IoProcessor processor, long sendBytesLen);
    
    void channelClose(IoProcessor processor);

	void channelError(IoProcessor processor, Throwable t);
}
