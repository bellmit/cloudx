package cloud.apposs.netkit.server;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.listener.IoListenerSupport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class ServerAcceptor extends Thread {
	public static final long SELECT_TIMEOUT = 1000L;
	
	/** 当前服务是否运行 */
	private volatile boolean running = false;
	
	private Selector selector;
	
	private IoServer server;
	
	public ServerAcceptor(IoServer server) {
		this.server = server;
		setName("ServerAcceptor");
	}
	
	@Override
	public void run() {
		try {
			InetSocketAddress bindAddr = server.getBindAddress();
			selector = Selector.open();
			server.doBind(selector, bindAddr);
		} catch (Throwable t) {
			Logger.error(t, "server create error, process exit!");
            System.exit(-1);
            return;
		}
		
		running = true;
		while(running) {
			int selected = 0;
			try {
				selected = selector.select(SELECT_TIMEOUT);
            } catch (Exception e) {
                Logger.error(e, "server select error");
                break;
            }
            // 处理连接的数据接收和发送
            if (selected > 0) {
            	Set<SelectionKey> readyKeys = selector.selectedKeys();
	            Iterator<SelectionKey> iter = readyKeys.iterator();
	            while (iter.hasNext()) {
	                SelectionKey key = iter.next();
	                iter.remove();
	                
	                if (key.isAcceptable() || key.isReadable()) {
	                    doAccept(key);
	                }
	            }
            }
		}
	}
	
	/**
	 * 处理新建立连接
	 */
	private void doAccept(SelectionKey key) {
		IoProcessor processor = null;
		try {
            processor = server.newProcessor(key.channel());
            server.getFilterChain().buildFilterChain(processor.getFilterChain());
            server.getServerListener().channelAccept(processor);
            EventChannel channel = processor.getChannel();
			processor.getFilterChain().fireChannelAccept(channel);
			IoListenerSupport listenerSupport = processor.getListenerSupport();
			if (listenerSupport != null) {
				processor.getListenerSupport().fireChannelAccept(processor);
			}
			key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
			server.getEventLoopGroup().addToLoop(processor);
		} catch(Throwable t) {
			if (processor != null) {
				IoListenerSupport listenerSupport = processor.getListenerSupport();
	    		if (listenerSupport != null) {
	    			listenerSupport.fireChannelError(processor, t);
	    		}
	    		processor.getFilterChain().fireExceptionCaught(t);
	    		
				if (listenerSupport != null) {
					processor.getListenerSupport().fireChannelClose(processor);
				}
				processor.getFilterChain().fireChannelClose();
			}
		}
	}
	
	public void shutdown() {
		running = false;
		try {
			selector.close();
		} catch (IOException e) {
		}
	}
}
