package cloud.apposs.netkit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Socket工具类，负责阻塞接收/发送网络数据
 * 注意，采用NIO模型的服务如果用阻塞接收/发送是只会降低服务性能反而不如用BIO，需要结合业务场景使用
 */
public class SocketUtil {
	/**
	 * 阻塞读取网络数据，timeout一定要定义避免一直阻塞网络请求导致性能急剧下降
	 */
	public static long recv(EventChannel channel, ByteBuffer buf, int expectedLen, int timeout) throws IOException {
		if (channel == null || buf == null || expectedLen <= 0 || timeout <= 0) {
			throw new IllegalArgumentException();
		}
		
		// buf里可接收的数据容量必须大于expectedLen
		if (buf.remaining() < expectedLen) {
			throw new IllegalArgumentException();
		}
		
		long beg = System.currentTimeMillis();
		long recvLen = 0;
		long totalLen = 0;
		
		Selector selector = null;
	    SelectionKey key = null;
		
		try {
			while (totalLen < expectedLen) {
				recvLen = channel.recv(buf);
				
				// 一开始就读取失败，可能远端已经关闭了连接
				if (recvLen < 0) {
					throw new IOException("recv error");
				}
				
				// 判断是否接收超时
				long end = System.currentTimeMillis();
	            if ((end - beg) > timeout) {
	            	throw new IOException("recv timeout");
	            }
	            
	            // 接收为0字节，此时可能还在等待对方传送数据，先注册到selector让内核触发接收事件避免cpu空转导致爆u
	            if (recvLen == 0) {
	            	if (selector == null) {
	                    selector = Selector.open();
	                    key = channel.register(selector, SelectionKey.OP_READ);
	                }
	            	selector.select(timeout);
	            }
				
				totalLen += recvLen;
	        }
		} finally {
			if (key != null) { 
	            key.cancel(); 
	            key = null; 
	        } 
	        if (selector != null) { 
	        	selector.selectNow(); 
	        	selector.close(); 
	        	selector = null;
	        }
		}
		
		return totalLen;
	}
	
	/**
	 * 阻塞发送网络数据，timeout一定要定义避免一直阻塞网络请求导致性能急剧下降
	 */
	public static long send(EventChannel channel, ByteBuffer buf, int needTransLen, int timeout) throws IOException {
		if (channel == null || buf == null || needTransLen <= 0 || timeout <= 0) {
			throw new IllegalArgumentException();
		}
		
		// buf里要发送的数据必须大于needTransLen
		if (buf.remaining() < needTransLen) {
			throw new IllegalArgumentException();
		}
		
		long beg = System.currentTimeMillis();
		long totalLen = 0;
		long sendLen = 0;
		
	    Selector selector = null;
	    SelectionKey key = null;
	    
	    try {
	    	while (totalLen < needTransLen) {
				// 判断是否接收超时
				long end = System.currentTimeMillis();
	            if ((end - beg) > timeout) {
	            	throw new IOException("send timeout");
	            }
	            
	            sendLen = channel.send(buf);
	            // 发送为0字节，网络发送有异常，可能内核网卡阻塞，注册到selector让内核触发发送事件避免cpu空转
	            if (sendLen == 0) {
	            	if (selector == null) {
	                    selector = Selector.open();
	                    key = channel.register(selector, SelectionKey.OP_WRITE);
	                }
	                selector.select(timeout);
	            }
	            
	            totalLen += sendLen;
			}
	    } finally {
			if (key != null) { 
	            key.cancel(); 
	            key = null; 
	        } 
	        if (selector != null) { 
	        	selector.selectNow(); 
	        	selector.close(); 
	        	selector = null;
	        }
		}
		
		return sendLen;
	}
}
