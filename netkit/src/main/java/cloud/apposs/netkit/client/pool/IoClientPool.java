package cloud.apposs.netkit.client.pool;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.client.ChannelFuture;
import cloud.apposs.netkit.client.ClientConfig;
import cloud.apposs.netkit.client.ClientHandler;
import cloud.apposs.netkit.client.IoClient;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterChainBuilder;
import cloud.apposs.netkit.listener.IoListenerSupport;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IO客户端连接池
 */
public class IoClientPool implements Serializable {
	private static final long serialVersionUID = -5263182382452784551L;
	
	private Class<? extends IoClient> clazz;
	
    private ClientPoolConfig config;
    
    private EventLoopGroup loop;
    
    private IoClientFactory factory;
    
    private final IoFilterChainBuilder filterChain = new IoFilterChainBuilder();
    
    /**
	 * 业务逻辑处理接口
	 */
    private ClientHandler handler;
    
	/** 网络连接池 */
	private final Queue<IoClientProxy> clients = new LinkedBlockingQueue<IoClientProxy>();
	
	/** 当前有多少个网络连接被创建 */
	private final AtomicInteger createds = new AtomicInteger(0);
	
	/** 连接池同步锁 */
    private final ReentrantLock mainLock = new ReentrantLock();
    
    /** 网络连接池是否正在运行 */
	private volatile boolean shutdown = false;
	
	public IoClientPool(Class<? extends IoClient> clazz) {
		this(null, clazz);
	}
	
	public IoClientPool(ClientPoolConfig config, Class<? extends IoClient> clazz) {
		this.clazz = clazz;
		if (config != null) {
			this.config = config;
			this.loop = doInitLoop();
		}
    }
	
    public boolean isRunning() {
		return !shutdown;
	}
    
    public IoClientPool clazz(Class<? extends IoClient> clazz) {
		if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (this.clazz != null) {
            throw new IllegalStateException("clazz set already");
        }
		this.clazz = clazz;
		return this;
	}

	public IoClientPool config(ClientPoolConfig config) {
		if (config == null) {
            throw new NullPointerException("config");
        }
        if (this.config != null) {
            throw new IllegalStateException("config set already");
        }
		this.config = config;
		return this;
	}
	
	public IoClientPool loop(EventLoopGroup loop) {
		if (loop == null) {
            throw new NullPointerException("loop");
        }
        if (this.loop != null) {
            throw new IllegalStateException("loop set already");
        }
        this.loop = loop;
        return this;
	}

	public IoClientPool factory(IoClientFactory factory) {
		if (factory == null) {
            throw new NullPointerException("factory");
        }
        if (this.factory != null) {
            throw new IllegalStateException("factory set already");
        }
		this.factory = factory;
		return this;
	}
	
	public IoClientPool filter(IoFilter filter) {
		SysUtil.checkNotNull(filter, "filter");
		filterChain.addFilter(filter);
		return this;
	}
	
	public IoClientPool handler(ClientHandler handler) {
		if (handler == null) {
            throw new NullPointerException("handler");
        }
        if (this.handler != null) {
            throw new IllegalStateException("handler set already");
        }
        this.handler = handler;
		return this;
	}
	
	/**
	 * 初始化并启动连接池
	 */
	public void start() throws IOException {
		if (clazz == null) {
			throw new IllegalStateException("clazz is not set");
		}
		if (config == null) {
			throw new IllegalStateException("config is not set");
		}
		
		if (factory == null) {
			factory = new DefaultIoClientFactory();
		}
		if (loop == null) {
			loop = doInitLoop();
		}
		loop.start();
	}
	
	/**
	 * 开始执行业务逻辑
	 */
	public ChannelFuture process() {
		if (handler == null) {
            throw new IllegalStateException("handler is not set");
        }
		
		FutureProcessorProxy processor = null;
		try {
			IoClientProxy client = doGetClient();
			if (client == null) {
				throw new IoClientException("Client Over Limit");
			}
			processor = client.getProcessor();
			if (processor == null) {
				throw new IoClientException("Client Connect Fail");
			}
			return processor.getFuture();
		} catch(Throwable t) {
			if (processor == null) {
				handler.channelError(t);
			} else {
				processor.getFilterChain().fireExceptionCaught(t);
	        	IoListenerSupport listenerSupport = processor.getListenerSupport();
	    		if (listenerSupport != null) {
	    			listenerSupport.fireChannelError(processor, t);
	    		}
			}
		}
		return null;
	}

	/**
     * 获取空闲的业务网络连接
     * 
     * @return 如果已经没有可用连接时返回null
     */
	private IoClientProxy doGetClient() throws Exception {
		// 从队列中取出连接
		IoClientProxy client = clients.poll();
		if (client != null) {
			IoProcessor processor = client.getProcessor();
			if (processor.selectionKey().isValid()) {
				// 已经建立连接，直接触发onConnect方法
				processor.getFilterChain().fireChannelConnect();
				IoListenerSupport listenerSupport = processor.getListenerSupport();
	    		if (listenerSupport != null) {
	    			listenerSupport.fireChannelConnect(processor);
	    		}
				return client;
			}
		}
		
		int maxPoolSize = config.getMaxPoolSize();
		// 空闲连接已经用完，继续创建指定连接
		int remain = maxPoolSize - createds.get();
		if (remain <= 0) {
			return null;
		}
		doCreateClient();
		return clients.poll();
	}
	
	/**
	 * 创建指定数量的网络连接句柄
	 */
	private int doCreateClient() throws Exception {
		// 创建固定数量的连接句柄
		int success = 0;
    	mainLock.lock();
        try {
        	IoClient client = factory.createClient(clazz);
        	if (client != null) {
        		ClientConfig clientCfg = doInitClientConfig();
        		IoClientProxy proxy = new IoClientProxy(client, this);
        		proxy.config(clientCfg).loop(loop).handler(handler);
        		proxy.initialize().connect();
        		clients.add(proxy);
        		success++;
	        	// 连接创建数递增
				createds.incrementAndGet();
        	}
        } catch(Exception e) {
        	throw new IoClientException("Create Client[" + clazz + "] Fail", e);
        } finally {
        	mainLock.unlock();
        }
        return success;
	}
	
	/**
	 * 初始化客户端配置
	 */
	private ClientConfig doInitClientConfig() {
		ClientConfig clientCfg = new ClientConfig();
		clientCfg.setConnectTimeout(config.getConnectTimeout());
		clientCfg.setRecvTimeout(config.getRecvTimeout());
		clientCfg.setSendTimeout(config.getSendTimeout());
		clientCfg.setTcpNoDelay(config.isTcpNoDelay());
		clientCfg.setHost(config.getHost());
		clientCfg.setPort(config.getPort());
		clientCfg.initialize();
		return clientCfg;
	}
	
	private EventLoopGroup doInitLoop() {
		String name = config.getName();
		String loopGroupName = "Event_Loop_IoClient_" + name + "_";
		int loopSize = config.getLoopPoolSize();
		return new EventLoopGroup(loopSize, loopGroupName, false);
	}
	
	/**
	 * 回收网络连接，为系统内部调用
	 */
	protected boolean retriveClient(IoClientProxy client) {
		if (client == null) {
			return false;
		}
		int maxPoolSize = config.getMaxPoolSize();
		if (createds.get() > maxPoolSize) {
			createds.decrementAndGet();
			client.internalClose();
		} else {
			clients.offer(client);
		}
		return true;
	}
	
	public void shutdownNow() {
		shutdown(true);
	}
	
	/**
	 * 销毁网络连接池
	 */
	public void shutdown() {
		shutdown(false);
	}
	
	/**
	 * 销毁网络连接池
	 */
	public void shutdown(boolean interrupt) {
		if (shutdown) {
			return;
		}
		mainLock.lock();
		try{
			shutdown = true;
			// 销毁所有IO轮询器
			loop.shutdown(interrupt);
			// 销毁所有Connection连接
			IoClientProxy client;
			while ((client = clients.poll()) != null) {
				client.internalClose();
				// Connection创建数递减
				createds.decrementAndGet();
			}
		} finally {
			mainLock.unlock();
		}
	}
	
	static class DefaultIoClientFactory implements IoClientFactory {
		@Override
		public IoClient createClient(Class<? extends IoClient> clazz) 
			throws Exception {
			return clazz.newInstance();
		}
	}
}
