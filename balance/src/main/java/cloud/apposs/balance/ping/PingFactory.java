package cloud.apposs.balance.ping;

import cloud.apposs.balance.IPing;
import cloud.apposs.balance.ping.HttpPing;
import cloud.apposs.balance.ping.NoOpPing;
import cloud.apposs.balance.ping.SocketPing;
import cloud.apposs.util.StrUtil;

public class PingFactory {
    /**
     * 不做任何PING检测
     */
    public static final String PING_TYPE_NOOP = "NoPing";
    /**
     * 基于TCP Socket的网络检测
     */
    public static final String PING_TYPE_SOCKET = "SocketPing";
    /**
     * 基于HTTP协议的请求检测
     */
    public static final String PING_TYPE_HTTP = "HttpPing";

    public static IPing createPing(String pingType) {
        if (StrUtil.isEmpty(pingType)) {
            throw new IllegalArgumentException();
        }
        if (pingType.equalsIgnoreCase(PING_TYPE_NOOP)) {
            return new NoOpPing();
        }
        if (pingType.equalsIgnoreCase(PING_TYPE_SOCKET)) {
            return new SocketPing();
        }
        if (pingType.equalsIgnoreCase(PING_TYPE_HTTP)) {
            return new HttpPing();
        }
        return null;
    }
}
