package cloud.apposs.util;

/**
 * 项目错误码
 */
public final class Errno {
	public static final int EOK = 0;
	public static final int EERROR = 1;
	public static final int EARGS_ERROR = 2;
	public static final int EAGAIN = 3;
	public static final int ENET_CONNECT_TIMEOUT = 4;
	public static final int ENET_RECV_TIMEOUT = 5;
	public static final int ENET_SEND_TIMEOUT = 6;
	public static final int ENET_ACCEPT_ERROR = 7;
	public static final int ENET_CONNECT_ERROR = 8;
	public static final int ENET_HANDSHAKE_ERROR = 9;
	public static final int ENET_RECV_ERROR = 10;
	public static final int ENET_SEND_ERROR = 11;
	public static final int EREADONLY = 12;
	public static final int EREQ_LIMIT = 13;
	public static final int EQUEUE_ERROR = 14;
	public static final int EOVER_LIMIT = 15;
	public static final int EKEEPALIVE_TIMEOUT = 16;
	public static final int EPROCESS_TIMEOUT = 17;
	public static final int ENULL = 18;
	public static final int ECLIENT_CLOSE = 19;
	
	private Errno() {
	}
}
