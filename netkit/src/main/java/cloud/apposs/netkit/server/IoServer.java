package cloud.apposs.netkit.server;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterChainBuilder;
import cloud.apposs.netkit.listener.IoListenerAdapter;
import cloud.apposs.netkit.listener.IoListenerSupport;
import cloud.apposs.netkit.schedule.ScheduledExecutor;
import cloud.apposs.netkit.server.annotation.AnnotationParser;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Server底层
 *
 * @date 2017.08.10
 */
public abstract class IoServer {
	/** 当前服务是否运行 */
	private volatile boolean running;
	
	/** 绑定的服务列表 */
	protected InetSocketAddress bindAddress;
	
	/**
	 * 数据处理的回调类
	 */
	protected ServerHandler handler;
	private final Map<Class<? extends Annotation>, AnnotationParser> annotationParsers =
		new ConcurrentHashMap<Class<? extends Annotation>, AnnotationParser>();
	
	protected ServerConfig config;
	
	protected EventLoopGroup eventLoopGroup;
	
	private ServerAcceptor acceptor;
	
	protected final ServerListener serverListener;
	protected final IoListenerSupport listenerSupport;
	protected final IoFilterChainBuilder filterChain;
	
	protected final ScheduledExecutor scheduleExecutor;
	
	public IoServer(ServerConfig config) {
		this(config, null);
	}
	
	public IoServer(ServerHandler handler) {
		this(null, handler);
	}
	
	public IoServer(ServerConfig config, ServerHandler handler) {
		this.config = config;
		this.handler = handler;
		this.serverListener = new ServerListener();
		this.listenerSupport = new IoListenerSupport();
		this.listenerSupport.add(serverListener);
		this.filterChain = new IoFilterChainBuilder();
		this.scheduleExecutor = new ScheduledExecutor(4);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void addAnnotationParser(AnnotationParser parser) {
		Class<? extends Annotation> annotationType = parser.getAnnotationType();
		if (!annotationParsers.containsKey(annotationType)) {
			annotationParsers.put(annotationType, parser);
		}
	}
	
	public InetSocketAddress getBindAddress() {
		if (bindAddress == null) {
			String host = config.getHost();
			int port = config.getPort();
			bindAddress = new InetSocketAddress(host, port);
		}
		return bindAddress;
	}

	public ServerHandler getHandler() {
		return handler;
	}

	public IoServer setHandler(ServerHandler handler) {
		this.handler = handler;
		return this;
	}

	public ServerListener getServerListener() {
		return serverListener;
	}

	public ServerConfig getConfig() {
		return config;
	}

	public EventLoopGroup getEventLoopGroup() {
		return eventLoopGroup;
	}

	public IoFilterChainBuilder getFilterChain() {
		return filterChain;
	}

	public ScheduledExecutor getScheduleExecutor() {
		return scheduleExecutor;
	}

	public void bind(String host, int port) {
		InetSocketAddress bindAddress = new InetSocketAddress(host, port);
		bind(bindAddress);
	}
	
	public void bind(InetSocketAddress bindAddress) {
		// 判断当前服务是否已经运行
		if (running) {
            throw new IllegalStateException("server already running");
        }
		
		this.bindAddress = bindAddress;
	}
	
	public IoServer config(ServerConfig config) {
		// 判断当前服务是否已经运行
		if (running) {
            throw new IllegalStateException("server already running");
        }
		
		this.config = config;
		return this;
	}
	
	public synchronized void start() throws IOException {
		if (running) {
            throw new IllegalStateException("server already running");
        }
		if (handler == null) {
			throw new IllegalStateException("handler is not set");
		}
		
		doParseServerAnnotation();
		doParseHandlerAnnotation();
		
		running = true;
		
		filterChain.initFilterChain();
		
		int numOfGroup = config.getNumOfGroup();
		boolean keepAlive = config.isKeepAlive();
		eventLoopGroup = new EventLoopGroup(numOfGroup, keepAlive);
		eventLoopGroup.start();
		
		acceptor = new ServerAcceptor(this);
		acceptor.start();
		
		serverListener.serverStart();
		registerDestory();
	}
	
	/**
	 * 解析{@link IoServer}的注解，
	 * 由各个{@link AnnotationParser}进行Server端的注解解析
	 */
	private void doParseServerAnnotation() {
		Annotation[] annotations = getClass().getAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			Annotation annotation = annotations[i];
			Class<? extends Annotation> annotationType = annotation.annotationType();
			AnnotationParser parser = annotationParsers.get(annotationType);
			if (parser != null) {
				parser.parse(this, annotation);
			}
		}
	}
	
	/**
	 * 解析{@link #handler}的注解，
	 * 由{@link ServerHandler}负责进行Handler端的注解解析
	 */
	private void doParseHandlerAnnotation() {
		handler.parseAnnotation(this, handler.getClass());
	}
	
	public synchronized void shutdown() {
		if (!running) {
			return;
		}
		
		running = false;
		if (acceptor != null) {
			acceptor.shutdown();
			acceptor = null;
		}
		if (eventLoopGroup != null) {
			eventLoopGroup.shutdown();
			eventLoopGroup = null;
		}
		
		filterChain.destroyFilterChain();
		serverListener.serverShutdown();
		scheduleExecutor.shutdown();
		
		destroy();
		
		IoAllocator.getAllocator().dispose();
	}
	
	abstract void destroy();
	
	public class ServerListener extends IoListenerAdapter {
		/** Server启动时间 */
		private long uptime = 0;
		
		/** 与服务器建立连接的客户端总数 */
		private long acceptedClients = 0;
		
		public ServerListener() {
		}
		
		public long getUptime() {
			return uptime;
		}
		
		public String getUptimDesc() {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return dateFormat.format(new Date(uptime));
		}
		
		public long getAcceptedClients() {
			return acceptedClients;
		}

		public void serverStart() {
			uptime = System.currentTimeMillis();
			String host = config.getHost();
			int port = config.getPort();
			Logger.info("Server Start Listening On %s:%d", host, port);
		}
		
		public void serverShutdown() {
			String host = config.getHost();
			int port = config.getPort();
			Logger.info("Server %s:%d Shutdown", host, port);
		}

		@Override
		public void channelAccept(IoProcessor processor) {
			acceptedClients++;
		}
	}
	
	/**
	 * 注册服务被kill时的回调，只能捕获kill -15的信号量 kill -9 没办法
	 */
	private void registerDestory() {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				shutdown();
			}
		});
	}
	
	@Override
	public String toString() {
		StringBuilder info = new StringBuilder();
		info.append("Server:\n");
		info.append("uptime     " + serverListener.getUptimDesc() + "\n");
		info.append("bindHost   " + config.getHost() + "\n");
		info.append("bindPort   " + config.getPort() + "\n");
		info.append("numOfGroup " + config.getNumOfGroup());
		info.append("acceptedClents " + serverListener.getAcceptedClients());
		return info.toString();
	}
	
	public abstract void doBind(Selector selector, InetSocketAddress bindAddr) throws IOException;
	
	public abstract IoProcessor newProcessor(SelectableChannel channel) throws IOException;
}
