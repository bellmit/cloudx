package cloud.apposs.netkit.client;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterChainBuilder;
import cloud.apposs.netkit.listener.IoListenerSupport;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 参考：
 * https://blog.csdn.net/tongdao/article/details/51417153
 * https://github.com/luxiaoxun/NettyRpc
 * https://blog.csdn.net/linshuhe1/article/details/72983545
 */
public abstract class IoClient {
	protected String name;
	
	/** 请求时传递的流水号，流水号始终存在于当次请求中 */
	protected int flow = 0;
	
	/**
	 * 业务逻辑处理接口
	 */
	protected ClientHandler handler;
	
	protected FutureProcessor processor;
	
	protected ClientConfig config;
	
	protected InetSocketAddress remoteAddr;
	
	/**
	 * IO轮询器，负责底层IO数据收发，
	 * 如果是同一个业务多个客户端发送请求，
	 * 注意该loop一定要外部传递，避免每次建立一次客户端请求即创建多一次线程
	 */
	protected EventLoopGroup loop;
	
	protected final IoFilterChainBuilder filterChain = new IoFilterChainBuilder();
	
	public IoClient() {
	}
	
	public IoClient(ClientConfig config, EventLoopGroup loop) throws IOException {
		this(config, loop, SysUtil.random());
	}
	
	public IoClient(ClientConfig config, EventLoopGroup loop, int flow) throws IOException {
		if (config == null || loop == null || flow <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.config = config;
		this.name = config.getName();
		this.flow = flow;
		this.loop = loop;
	}
	
	public String name() {
		return name;
	}

	public int flow() {
		return flow;
	}

	public IoClient flow(int flow) {
		this.flow = flow;
		return this;
	}

	public ClientHandler handler() {
		return handler;
	}

	public IoClient handler(ClientHandler handler) {
		if (handler == null) {
            throw new NullPointerException("handler");
        }
        if (this.handler != null) {
            throw new IllegalStateException("handler set already");
        }
        this.handler = handler;
		return this;
	}

	public ClientConfig config() {
		return config;
	}

	public IoClient config(ClientConfig config) {
		if (config == null) {
            throw new NullPointerException("config");
        }
        if (this.config != null) {
            throw new IllegalStateException("config set already");
        }
		this.config = config;
		return this;
	}

	public IoClient loop(EventLoopGroup loop) {
		if (loop == null) {
            throw new NullPointerException("loop");
        }
        if (this.loop != null) {
            throw new IllegalStateException("loop set already");
        }
        this.loop = loop;
        return this;
	}
	
	public IoFilterChainBuilder filterChain() {
		return filterChain;
	}
	
	public IoClient filter(IoFilter filter) {
		SysUtil.checkNotNull(filter, "filter");
		filterChain.addFilter(filter);
		return this;
	}

	public InetSocketAddress getRemoteAddr() {
		if (remoteAddr == null) {
			String host = config.getHost();
			int port = config.getPort();
			remoteAddr = new InetSocketAddress(host, port);
		}
		return remoteAddr;
	}
	
	public ChannelFuture connect() {
		return connect(getRemoteAddr());
	}
	
	public ChannelFuture connect(String host, int port) {
		return connect(new InetSocketAddress(host, port));
	}
	
	/**
	 * 和远程服务器建立连接
	 * 
	 * @param  remoteAddr 远程服务器地址
	 * @return 创建连接成功返回ChannelFuture，不成功返回NULL
	 */
	public ChannelFuture connect(InetSocketAddress remoteAddr) {
		try {
			this.remoteAddr = remoteAddr;
			processor = newProcessor(remoteAddr);
			if (processor == null) {
				handler.channelError(new IOException("IoProcessor Create Fail"));
				return null;
			}
			filterChain.buildFilterChain(processor.getFilterChain());
			loop.addToLoop(processor);
		} catch(Throwable t) {
			processor.getFilterChain().fireExceptionCaught(t);
        	IoListenerSupport listenerSupport = processor.getListenerSupport();
    		if (listenerSupport != null) {
    			listenerSupport.fireChannelError(processor, t);
    		}
		}
		return processor.getFuture();
	}

	/**
	 * TCP/UDP/连接复用，均在此实现，可在此实现连接池复用连接
	 */
	public abstract FutureProcessor newProcessor(InetSocketAddress addr) throws IOException;
}
