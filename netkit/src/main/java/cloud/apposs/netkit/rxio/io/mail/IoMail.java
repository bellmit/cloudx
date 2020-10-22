package cloud.apposs.netkit.rxio.io.mail;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventSocketChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.socks.SocksFilter;
import cloud.apposs.netkit.filterchain.ssl.SslFilter;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.util.SysUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class IoMail extends AbstractIoProcessor {
	public static final int STEP_1_WELCOME = 0;
	public static final int STEP_2_EHLO = 1;
	public static final int STEP_3_MAILFROM = 2;
	public static final int STEP_4_RCPTTO = 3;
	public static final int STEP_5_DATA = 4;
	public static final int STEP_6_QUIT = 5;
	public static final int STEP_7_BYE = 6;
	
	public static final int SMTP_PORT = 25;
	public static final int SMTP_PORT_SSL = 465;
	
	public static final String RESPONSE_WELCOME = "220";
	public static final String RESPONSE_OK = "250";
	public static final String RESPONSE_WAITDATA = "354";
	public static final String RESPONSE_QUIT = "221";
	
	public static final String CRLF = "\r\n";
	public static final String CRLFDOCRLF = "\r\n.\r\n";
	
	private InetSocketAddress addr;
	
	private Proxy proxy;
	
	private EventSocketChannel channel;
	
	/** 邮件发送的编码 */
	private Charset charset = Charset.forName("utf-8");
	
	/** 当前异步请求步骤 */
	private int step = STEP_1_WELCOME;
	private String cmd = "EHLO";
	
	/** 读取到的服务器欢迎信息 */
	private String welcome;
	/** 远程mx服务器最后成功/失败的结果 */
	private String lastResp;
	
	/** ehlo/helo时的招呼 */
	private String copyright = "JavaMail Processor1.0";
	/** 发件人 */
	private String from;
	/** 收件人 */
	private String to;
	/** data命令发送的邮件数据 */
	private IoBuffer data;
	
	public IoMail(String host, int port) throws Exception {
		this(new InetSocketAddress(host, port), null);
	}
	
	public IoMail(String host, int port, Proxy proxy) throws Exception {
		this(new InetSocketAddress(host, port), proxy);
	}
	
	public IoMail(InetSocketAddress addr) throws Exception {
		this(addr, null);
	}
	
	public IoMail(InetSocketAddress addr, Proxy proxy) throws Exception {
		SysUtil.checkNotNull(addr, "addr");
		
		this.addr = addr;
		if (proxy != null) {
			chain.add(new SocksFilter());
		}
		if (addr.getPort() == SMTP_PORT_SSL) {
			chain.add(new SslFilter(true, false));
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void channelRead(Object msg) throws Exception {
		IoBuffer buffer = (IoBuffer) msg;
		lastResp = buffer.string(charset);
		boolean ok = false;
		
		try {
			switch(step) {
			case STEP_1_WELCOME:
				// 获取远程服务器发送的欢迎信息
				welcome = lastResp;
				// 发送ehlo/helo招呼信息
				cmd = "EHLO " + copyright;
				write(cmd + CRLF, charset);
				ok = true;
				step = STEP_2_EHLO;
				break;
			case STEP_2_EHLO:
				ok = isResponseOk(lastResp, RESPONSE_OK);
				if (ok) {
					String cmd = "MAIL FROM:<" + from + ">";
					write(cmd + CRLF, charset);
					step = STEP_3_MAILFROM;
				}
				break;
			case STEP_3_MAILFROM:
				ok = isResponseOk(lastResp, RESPONSE_OK);
				if (ok) {
					cmd = "RCPT TO:<" + to + ">";
					write(cmd + CRLF, charset);
					step = STEP_4_RCPTTO;
				}
				break;
			case STEP_4_RCPTTO:
				ok = isResponseOk(lastResp, RESPONSE_OK);
				if (ok) {
					cmd = "DATA";
					write(cmd + CRLF, charset);
					step = STEP_5_DATA;
				}
				break;
			case STEP_5_DATA:
				ok = isResponseOk(lastResp, RESPONSE_WAITDATA);
				if (ok) {
					data.put(CRLFDOCRLF.getBytes());
					write(data);
					step = STEP_6_QUIT;
				}
				break;
			case STEP_6_QUIT:
				ok = isResponseOk(lastResp, RESPONSE_OK);
				if (ok) {
					cmd = "QUIT";
					write(cmd + CRLF, charset);
					step = STEP_7_BYE;
				}
				break;
			case STEP_7_BYE:
				try {
					ok = isResponseOk(lastResp, RESPONSE_QUIT);
					if (ok) {
						// 该代码主要服务响应式异步调用
						if (context instanceof IoSubscriber) {
							IoSubscriber subscribe = (IoSubscriber) getContext();
							subscribe.onNext(new MailResult(ok, lastResp));
						}
					}
				} finally {
					close(true);
				}
				break;
			}
		} finally {
			if (!ok) {
				// 整个邮件发送失败，回调业务方代码
				if (context instanceof IoSubscriber) {
					IoSubscriber subscribe = (IoSubscriber) getContext();
					subscribe.onNext(new MailResult(ok, lastResp));
				}
			}
		}
	}

	@Override
	public EventChannel getChannel() {
		return channel;
	}

	@Override
	public SelectionKey doRegister(Selector selector) throws IOException {
		InetSocketAddress addr = this.addr;
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

	public String getWelcome() {
		return welcome;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}
	
	public void setFrom(String from) {
		this.from = from;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public void setData(IoBuffer data) {
		this.data = data;
	}

	private static boolean isResponseOk(String response, String... exceptedResps) {
		for (String exceptedResp : exceptedResps) {
			if (response.startsWith(exceptedResp)) {
	    		return true;
	    	}
		}
    	return false;
	}
}
