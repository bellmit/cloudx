package cloud.apposs.netkit;

import cloud.apposs.netkit.buffer.Allocator;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.buffer.MappedAllocator;
import cloud.apposs.netkit.buffer.SimpleAllocator;
import cloud.apposs.netkit.buffer.ZeroCopyAllocator;
import cloud.apposs.netkit.filterchain.executor.ThreadPool;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolFilter;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolHandler;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolType;
import cloud.apposs.netkit.filterchain.line.TextLineFilter;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.io.whois.IoWhois;
import cloud.apposs.netkit.rxio.io.whois.WhoisInfo;
import cloud.apposs.netkit.server.ServerAction;
import cloud.apposs.netkit.server.ServerConfig;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;
import cloud.apposs.netkit.server.TcpServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestTcpServer {
	public static final String SERVER_CONF = "server-config.xml";
	public static final int USE_ZERO_COPY = 1;
	public static final int USE_FILE_MAPPED = 2;
	public static final boolean USE_THREAD = true;

	public static void main(String[] args) throws Exception {
		Allocator allocator = null;
		int mode = -1;
		if (mode == USE_ZERO_COPY) {
			allocator = new ZeroCopyAllocator(12, "D:/Tmp/mapped/");
		} else if (mode == USE_FILE_MAPPED) {
			allocator = new MappedAllocator(12, "D:/Tmp/mapped/");
		} else {
			allocator = new SimpleAllocator();
		}
		IoAllocator.setAllocator(allocator);
		
		ServerConfig config = new ServerConfig(SERVER_CONF);
		config.setRecvTimeout(8000);
		config.setSendTimeout(8000);
		TcpServer server = new TcpServer(config);
		server.getFilterChain().addFilter(new TextLineFilter());
		if (USE_THREAD) {
			server.getFilterChain().addFilter(new ThreadPoolFilter(new MyThreadPoolHandler()));
		}
		server.setHandler(new HelloWorldHandler());
		server.start();
	}
	
	static class HelloWorldHandler extends ServerHandlerAdaptor {
		@Override
		public byte[] getWelcome() {
			return "Welcome To Server\r\n".getBytes();
		}

		@Override
		public void channelRead(ServerHandlerContext context, Object msg) throws Exception {
			IoBuffer buf = (IoBuffer) msg;
			String info = buf.string();
			if (info.equalsIgnoreCase("quit")) {
				System.out.println("Session close");
				context.close(true);
				return;
			}
			
			context.write("Hello Client:" + info + ";Thread:" + Thread.currentThread() +"\r\n");
			System.out.println(info);
		}

		@Override
		public void channelError(ServerHandlerContext context, Throwable cause) {
			cause.printStackTrace();
		}
	}
	
	static class BatchSendHandler extends ServerHandlerAdaptor {
		@Override
		public byte[] getWelcome() {
			return "Welcome BatchSend Server\r\n".getBytes();
		}

		@Override
		public void channelRead(ServerHandlerContext context, Object msg) throws Exception {
			IoBuffer buf = (IoBuffer) msg;
			String info = buf.string();
			if (info.equalsIgnoreCase("quit")) {
				System.out.println("Session close");
				context.close(true);
				return;
			}
			if (info.equalsIgnoreCase("igc")) {
				System.out.println("Session gc");
				System.gc();
				context.write("System GC\r\n");
				return;
			}
			
			IoBuffer buf1 = ByteBuf.wrap("Hello".getBytes());
			IoBuffer buf2 = ByteBuf.wrap("Client".getBytes());
			IoBuffer buf3 = ByteBuf.wrap(":".getBytes());
			IoBuffer buf4 = ByteBuf.wrap(info.getBytes());
			IoBuffer buf5 = ByteBuf.wrap("\r\n".getBytes());
			context.write(buf1, buf2, buf3, buf4, buf5);
			System.out.println(info);
		}

		@Override
		public void channelError(ServerHandlerContext context, Throwable cause) {
			System.out.println("handler exception caught:");
			cause.printStackTrace();
		}
	}
	
	static class WhoisHandler extends ServerHandlerAdaptor {
		@Override
		public byte[] getWelcome() {
			return "Welcome To Whois Server\r\n".getBytes();
		}

		@Override
		public void channelRead(final ServerHandlerContext context, final Object msg) throws Exception {
			IoBuffer buf = (IoBuffer) msg;
			final String domain = buf.string();
			System.out.println("Main Thread:" + Thread.currentThread());
			if (domain.equalsIgnoreCase("quit")) {
				context.close(true);
				return;
			}
			if (domain.equalsIgnoreCase("wrong.com")) {
				throw new IllegalArgumentException(domain);
			}
			
			final EventLoopGroup group = context.getLoopGroup();
			// 异步通过whois查询域名信息
			RxIo<WhoisInfo> request = RxIo.create(group, new IoWhois(domain));
			request.subscribe(new ServerAction<WhoisInfo>(this, context) {
				@Override
				public void onHandle(WhoisInfo value) throws Exception {
					System.out.println("RxIo Thread:" + Thread.currentThread());
					context.write(value + "\r\n");
				}
			}).start();
		}
		
		@Override
		public void channelError(ServerHandlerContext context, Throwable cause) {
			cause.printStackTrace();
			try {
				context.write("System Error:" + cause.getMessage() + "\r\n");
			} catch (IOException e) {
			}
			context.close(false);
		}
	}
	
	static class TimeoutHandler extends ServerHandlerAdaptor {
		@Override
		public byte[] getWelcome() {
			return "Welcome To Whois Server\r\n".getBytes();
		}

		@Override
		public void channelRead(final ServerHandlerContext context, final Object msg) throws Exception {
			IoBuffer buf = (IoBuffer) msg;
			final String domain = buf.string();
			System.out.println("Thread:" + Thread.currentThread());
			if (domain.equalsIgnoreCase("quit")) {
				context.close(true);
				return;
			}
			Thread.sleep(10000);
		}
		
		@Override
		public void channelError(ServerHandlerContext context, Throwable cause) {
			cause.printStackTrace();
			try {
				context.write("System Error:" + cause.getMessage() + "\r\n");
			} catch (IOException e) {
			}
			context.close(false);
		}
	}
	
	static class OneRequestHandler extends ServerHandlerAdaptor {
		@Override
		public void channelRead(final ServerHandlerContext context,
				final Object message) throws Exception {
			IoBuffer buf = (IoBuffer) message;
			String info = buf.string();
			System.out.println("Thread:" + Thread.currentThread() + " " + info);
			context.write("Hello Client:" + info + ";Thread:" + Thread.currentThread() +"\r\n");
			context.close(false);
		}
		
		@Override
		public void channelError(ServerHandlerContext context, Throwable cause) {
			cause.printStackTrace();
			try {
				context.write("System Error:" + cause.getMessage() + "\r\n");
			} catch (IOException e) {
			}
			context.close(false);
		}
	}
	
	private static class MyThreadPoolHandler implements ThreadPoolHandler {
		@Override
		public List<ThreadPool> createPoolGroups() {
			List<ThreadPool> pools = new ArrayList<ThreadPool>();
			pools.add(new ThreadPool("MyPool", 2));
			return pools;
		}
		
		@Override
		public ThreadPoolType getThreadPoolType(Object message) {
			return new ThreadPoolType("MyPool", null);
		}
	}
}
