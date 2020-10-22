package cloud.apposs.netkit.filterchain.socks;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.SocketUtil;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;

/**
 * Socks代理过滤器，仅适用于客户端
 */
public class SocksFilter extends IoFilterAdaptor {
	public static final String FILTER_CONTEXT = "SocksFilterContext";
	
	public InetSocketAddress channelProxy(IoProcessor processor, Proxy proxy, InetSocketAddress source) {
		Context context = new Context(proxy, source);
        processor.setAttribute(FILTER_CONTEXT, context);
        return context.proxyAddr();
	}
	
	@Override
	public void channelConnect(IoFilter.NextFilter nextFilter, IoProcessor processor) throws Exception {
		Context context = getContext(processor);
		context.handshake(processor.getChannel());
		nextFilter.channelConnect(processor);
	}

	private Context getContext(IoProcessor processor) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        return context;
    }
	
	public static class Context {
		private static int HANDSHAKE_TIMEOUT = 500;
		
		/** VER协议版本（4/5） */
		public static final byte SOCKS_V4 = 0x04;
		public static final byte SOCKS_V5 = 0x05;
		
		/** METHOD认证方法 */
		/** 不需要认证 */
		public static final byte METHOD_NO_AUTH = 0x00;
		public final static byte[] SUPPORTED_AUTH_METHODS = new byte[]{METHOD_NO_AUTH};
		
		public static final byte CMD_CONNECT = 0x01;
		public static final byte RSV = 0x00;
		public static final byte ATYPE_IPV4 = 0x1;
	    public static final byte ATYPE_IPV6 = 0x04;
		
		/** 远程socks5代理服务器 */
		private Proxy proxy;
		
		/** 要请求的真正远程地址 */
		private InetSocketAddress source;
		
		public Context(Proxy proxy, InetSocketAddress source) {
			this.proxy = proxy;
			this.source = source;
		}
		
		public void handshake(EventChannel channel) throws IOException {
			ByteBuffer sendBuf = null;
			ByteBuffer recvBuf = null;
			int sendLen = 0;
			int recvLen = 0;
			
			// 发送请求协商版本和认证方法
			sendBuf = encodeGreetingPacket(SOCKS_V5);
			sendBuf.flip();
			sendLen = sendBuf.limit();
			SocketUtil.send(channel, sendBuf, sendLen, HANDSHAKE_TIMEOUT);
			sendBuf = null;
			
			// 获取服务器返回协商结果
			recvLen = 2;
			recvBuf = ByteBuffer.allocate(recvLen);
			SocketUtil.recv(channel, recvBuf, recvLen, HANDSHAKE_TIMEOUT);
			byte version = recvBuf.get(0);
			byte method = recvBuf.get(1);
			if (version != SOCKS_V5 && method != METHOD_NO_AUTH) {
				throw new IOException("socks version and method not matched");
			}
			
			// 发送要建立连接的远程ip和端口
			sendBuf = encodeProxyPacket(SOCKS_V5, CMD_CONNECT, source);
			sendBuf.flip();
			sendLen = sendBuf.limit();
			recvLen = sendBuf.limit();
			SocketUtil.send(channel, sendBuf, sendLen, HANDSHAKE_TIMEOUT);
			sendBuf = null;
			
			// 获取服务器返回的处理结果
			recvBuf = ByteBuffer.allocate(recvLen);
			SocketUtil.recv(channel, recvBuf, recvLen, HANDSHAKE_TIMEOUT);
		}
		
		/**
		 * socks5协商阶段数据包组包
		 */
		private ByteBuffer encodeGreetingPacket(byte version) {
			byte nbMethods = (byte) SUPPORTED_AUTH_METHODS.length;
			ByteBuffer buf = ByteBuffer.allocate(2 + nbMethods);
			
			buf.put(version);
	        buf.put(nbMethods);
	        buf.put(SUPPORTED_AUTH_METHODS);

	        return buf;
		}
		
		/**
		 * socks5建立代理连接阶段数据包组包
		 */
		private ByteBuffer encodeProxyPacket(byte version, byte cmd, InetSocketAddress addr) {
			int len = 6;
			byte addressType = 0;
			int port = addr.getPort();
			
			if (addr.getAddress() instanceof Inet6Address) {
	            len += 16;
	            addressType = ATYPE_IPV6;
	        } else if (addr.getAddress() instanceof Inet4Address) {
	            len += 4;
	            addressType = ATYPE_IPV4;
	        }
			
			ByteBuffer buf = ByteBuffer.allocate(len);
			buf.put(version);
	        buf.put(cmd);
	        buf.put(RSV);
	        buf.put(addressType);
	        buf.put(addr.getAddress().getAddress());
	        buf.put((byte)(port >> 8));
	        buf.put((byte)(port));
			
			return buf;
		}
		
		public InetSocketAddress proxyAddr() {
			return (InetSocketAddress) proxy.address();
		}
	}
}
