package cloud.apposs.balance;

/**
 * 后端线路存活检测服务，例如HTTP/TCP/RPC等不同方式存储检测
 */
public interface IPing {
	/**
	 * 检测指定后端服务是否存活
	 */
	boolean isAlive(Peer peer);
}
