package cloud.apposs.balance;

import java.util.EventListener;

public interface PeerListener extends EventListener {
	/**
	 * 添加后端监听时的监听
	 */
	void peerAdded(final Peer peer);
	
	/**
	 * 后端状态变更时的监听
	 */
	void peerChanged(final Peer peer);
}
