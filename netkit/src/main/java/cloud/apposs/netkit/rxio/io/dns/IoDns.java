package cloud.apposs.netkit.rxio.io.dns;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventDatagramChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.filterchain.dns.DnsFilter;
import cloud.apposs.netkit.filterchain.dns.DnsMessage;
import cloud.apposs.netkit.filterchain.dns.IoBufferAccessor;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * DNS异步解析，参考
 * http://www.ruanyifeng.com/blog/2018/05/root-domain.html
 * https://blog.csdn.net/tianxuhong/article/details/74922454
 * http://www.cnblogs.com/pied/p/3571055.html
 * https://tools.ietf.org/html/rfc1035  RFC 文档
 * https://en.wikipedia.org/wiki/List_of_DNS_record_types 资源记录类型、返回值、操作码等说明的维基文档
 * http://help.dnsmadeeasy.com/managed-dns/dns-record-types/ns-record/  资源记录文档，修改最后一个目录可以查看不同资源记录
 * 
 * 1、先通过/etc/resolv.conf获取dns服务，默认为114.114.114.114/8.8.8.8/8.8.4.4<br>
 * 2、通过/etc/hosts或者/system32/drivers/etc/hosts获取hosts文件<br>
 * 3、如果hosts文件不存在该映射则通过网络递归获取<br>
 */
public class IoDns extends AbstractIoProcessor {
	public static final String DNS_IP = "114.114.114.114";
	public static final int DNS_PORT = 53;
	
	private String resolveIp = DNS_IP;
	
	private int resolvePort = DNS_PORT;
	
	private DnsMessage message;
	
	private EventDatagramChannel channel;
	
	public IoDns(String domain, int type) {
	    this(DnsMessage.newQuery(domain, type), DNS_IP, DNS_PORT);
    }

	public IoDns(String domain, int type, String ip) {
	    this(DnsMessage.newQuery(domain, type), ip, DNS_PORT);
    }
	
    public IoDns(DnsMessage message) {
        this(message, DNS_IP, DNS_PORT);
    }

	public IoDns(DnsMessage message, String ip) {
		this(message, ip, DNS_PORT);
	}

	public IoDns(DnsMessage message, String ip, int port) {
		if (StrUtil.isEmpty(ip) || port <= 0 || message == null) {
			throw new IllegalArgumentException(String.format("ip=%s;port=%s;msg=%s", ip, port, message));
		}
        this.message = message;
		this.resolveIp = ip;
		this.resolvePort = port;
        chain.add(new DnsFilter(this, message.getHeader().getId()));
	}
	
	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		DatagramChannel datagramChannel = DatagramChannel.open();
		datagramChannel.configureBlocking(false);
		InetSocketAddress addr = new InetSocketAddress(resolveIp, resolvePort);
		channel = new EventDatagramChannel(datagramChannel);
		channel.connect(addr);
		write(buildDnsRequest());
		return channel.register(selector, SelectionKey.OP_WRITE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void channelRead(Object response) throws Exception {
		try {
			if (!(response instanceof DnsMessage)) {
                return;
            }
			DnsMessage dnsPacket = ((DnsMessage) response);
			if (dnsPacket.getHeader().getId() != message.getHeader().getId()) {
				return;
			}
			
			// 该代码主要服务响应式异步调用
            if (context instanceof IoSubscriber) {
            	IoSubscriber subscribe = (IoSubscriber) getContext();
                subscribe.onNext(response);
            }
		} finally {
			close(true);
		}
	}

	@Override
	public EventChannel getChannel() {
		return channel;
	}
	
	/**
	 * 编码DNS请求包发送到网络中
	 */
	private IoBuffer buildDnsRequest() throws IOException {
		IoBufferAccessor accessor = new IoBufferAccessor(new ByteBuf(32, false));
        message.toWire(accessor);
        return accessor.getBuffer();
	}
}
