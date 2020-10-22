package cloud.apposs.netkit.rxio.io.whois;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventSocketChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.socks.SocksFilter;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Whois异步网络查询，查询返回的数据为String类型
 */
public class IoWhois extends AbstractIoProcessor {
	private static Map<String, Pair<String, String>> WHOIS_REGISTRY =
		new HashMap<String, Pair<String, String>>();
	static {
		WHOIS_REGISTRY.put(".com", new Pair<String, String>("whois.internic.net", "no match"));
		WHOIS_REGISTRY.put(".net", new Pair<String, String>("whois.internic.net", "no match"));
		WHOIS_REGISTRY.put(".edu", new Pair<String, String>("whois.internic.net", "no match"));
		WHOIS_REGISTRY.put(".org", new Pair<String, String>("whois.publicinterestregistry.net", "not found"));
		WHOIS_REGISTRY.put(".cn", new Pair<String, String>("whois.cnnic.cn", "no matching"));
		WHOIS_REGISTRY.put(".cc", new Pair<String, String>("whois.nic.cc", "no match"));
		WHOIS_REGISTRY.put(".co", new Pair<String, String>("whois.nic.co", "not found"));
		WHOIS_REGISTRY.put(".biz", new Pair<String, String>("whois.neulevel.biz", "not found"));
		WHOIS_REGISTRY.put(".pro", new Pair<String, String>("whois.registrypro.pro", "not found"));
		WHOIS_REGISTRY.put(".mobi", new Pair<String, String>("whois.dotmobiregistry.net", "not found"));
		WHOIS_REGISTRY.put(".me", new Pair<String, String>("whois.nic.me", "not found"));
		WHOIS_REGISTRY.put(".in", new Pair<String, String>("whois.inregistry.in", "not found"));
		WHOIS_REGISTRY.put(".uk", new Pair<String, String>("whois.nic.uk", "no match"));
		WHOIS_REGISTRY.put(".us", new Pair<String, String>("whois.nic.us", "no found"));
		WHOIS_REGISTRY.put(".info", new Pair<String, String>("whois.afilias.info", "no found"));
		WHOIS_REGISTRY.put(".de", new Pair<String, String>("whois.denic.de", "no found"));
		WHOIS_REGISTRY.put(".ca", new Pair<String, String>("whois.cira.ca", "no found"));
		WHOIS_REGISTRY.put(".top", new Pair<String, String>("whois.internic.net", "no match"));
	}
	public static final int WHOIS_PORT = 43;
	public static final String CHARSET = "utf-8";
	
	private EventSocketChannel channel;
	
	private final String domain;
	
	private final Proxy proxy;
	
	private final StringBuilder response = new StringBuilder(128);
	
	public IoWhois(String domain) {
		this(domain, null);
	}
	
	public IoWhois(String domain, Proxy proxy) {
		this.domain = domain;
		this.proxy = proxy;
		
		if (proxy != null) {
			chain.add(new SocksFilter());
		}
	}

	@Override
	public void channelConnect() throws IOException {
		write((domain + "\r\n"), Charset.forName("utf-8"));
	}
	
	@Override
	public void channelRead(Object message) throws Exception {
		IoBuffer buffer = (IoBuffer) message;
		response.append(buffer.string(Charset.forName(CHARSET)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void channelReadEof(Object message) throws Exception {
		String whoisStr = response.toString();
		boolean isReg = isRegisted(whoisStr);
		WhoisInfo whoisInfo = new WhoisInfo(domain, whoisStr, isReg);
		
		// 该代码主要服务响应式异步调用
		if (context instanceof IoSubscriber) {
			IoSubscriber subscribe = (IoSubscriber) getContext();
			subscribe.onNext(whoisInfo);
		}
	}

	private boolean isRegisted(String info) {
		Pair<String, String> registry = getRegistry(domain);
		if (info == null || registry == null) {
			return false;
		}
		String nomatch = registry.second();
		return !info.toLowerCase().contains(nomatch);
	}

	@Override
	public EventChannel getChannel() {
		return channel;
	}

	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		Pair<String, String> registry = getRegistry(domain);
		if (registry == null) {
			throw new IOException("domain " + domain + " whois server not found");
		}
		InetSocketAddress addr = new InetSocketAddress(
				InetAddress.getByName(registry.first()), WHOIS_PORT);
		
		// 判断是否使用代理，用代理则需要重写连接地址
		if (proxy != null) {
			SocksFilter socks = (SocksFilter) chain.get(SocksFilter.class);
			if (socks != null) {
				addr = socks.channelProxy(this, proxy, addr);
			}
		}
		
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		socketChannel.socket().setTcpNoDelay(true);
		
		channel = new EventSocketChannel(socketChannel);
		channel.connect(addr);
		return channel.register(selector, SelectionKey.OP_CONNECT);
	}
	
	public static Pair<String, String> getRegistry(String domain) {
		if (domain == null || domain.trim().isEmpty()) {
			return null;
		}
		
		for (Entry<String, Pair<String, String>> set : WHOIS_REGISTRY.entrySet()) {
			if (domain.toLowerCase().endsWith(set.getKey())) {
				return set.getValue();
			}
		}
		
		return null;
	}
	
	public static void addRegistry(String key, Pair<String, String> registry) {
		WHOIS_REGISTRY.put(key, registry);
	}
}
