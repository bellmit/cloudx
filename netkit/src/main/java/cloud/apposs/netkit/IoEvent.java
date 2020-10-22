package cloud.apposs.netkit;

import java.nio.channels.SelectionKey;


/**
 * 网络事件封装，实时记录着{@link IoProcessor}对网络数据的收发状态
 * 底层{@link EventLoop}会根据该事件来判断修改SelectionKey注册事件
 */
public class IoEvent {
	private int event = 0;
	
	/** 读取网络数据事件 */
    public static final int OP_READ = 1 << 0;
    
    /** 发送网络数据事件 */
    public static final int OP_WRITE = 1 << 2;

    /** 建立服务端连接事件 */
    public static final int OP_CONNECT = 1 << 3;

    /** 客户端建立连接事件 */
    public static final int OP_ACCEPT = 1 << 4;
    
    /** 请求是否正在关闭事件，例如超时如果业务方需要发送超时消息给客户端再关闭请求时则为此状态 */
    public static final int OP_CLOSING = 1 << 5;
    
    /** 直接关闭请求事件 */
    public static final int OP_CLOSE = 1 << 6;
    
	public final int getEvent() {
		return event;
	}

	public final void setEvent(int event) {
		this.event = event;
	}
	
	public final boolean isEventInterest(int interestEvent) {
		return (event & interestEvent) == interestEvent;
	}
	
	public final IoEvent interestEvent(int event) {
		this.event |= event;
		return this;
	}
	
	public final IoEvent unInterestEvent(int event) {
		this.event &= ~event;
		return this;
	}
	
	public final static boolean isSelectionKeyEventInterest(int keyInterestOps, int keyEvent) {
		return (keyInterestOps & keyEvent) == keyEvent;
	}
	
	public final static IoEvent registSelectionKeyEvent(IoEvent event, SelectionKey key, int keyEvent) {
		// 直接关闭会话事件
		if (keyEvent == IoEvent.OP_CLOSE) {
			event.setEvent(IoEvent.OP_CLOSE);
			if (key != null) {
				key.selector().wakeup();
			}
			return event;
		}
		
		// 发送错误信息后再关闭会话事件
		if (keyEvent == IoEvent.OP_CLOSING) {
			// 转换写事件触发EventLoop.doSend(SelectionKey)发送数据
			event.interestEvent(IoEvent.OP_WRITE).interestEvent(IoEvent.OP_CLOSING);
			// 同时直接在Selector注册写事件马上触发数据发送，
			// 这样做在于当调用该发送操作的逻辑在另外的线程时也可以立即触发EventLoop线程内的数据发送
			if (key != null) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
				key.selector().wakeup();
			}
			return event;
		}
		
		// 其他事件，包括读、写、连接、接收事件
		event.interestEvent(keyEvent);
		if (key != null) {
			key.interestOps(key.interestOps() | keyEvent);
			key.selector().wakeup();
		}
		return event;
	}
	
	public final static IoEvent unRegistSelectionKeyEvent(IoEvent event, SelectionKey key, int keyEvent) {
		event.unInterestEvent(keyEvent);
		if (key != null) {
			key.interestOps(key.interestOps() & (~keyEvent));
			key.selector().wakeup();
		}
		return event;
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder(32);
		if (isEventInterest(OP_READ)) {
			info.append("OP_READ ");
		}
		if (isEventInterest(OP_WRITE)) {
			info.append("OP_WRITE ");
		}
		if (isEventInterest(OP_CONNECT)) {
			info.append("OP_CONNECT ");
		}
		if (isEventInterest(OP_ACCEPT)) {
			info.append("OP_ACCEPT ");
		}
		if (isEventInterest(OP_CLOSING)) {
			info.append("OP_CLOSING ");
		}
		if (isEventInterest(OP_CLOSE)) {
			info.append("OP_CLOSE ");
		}
		return info.toString();
	}
}
