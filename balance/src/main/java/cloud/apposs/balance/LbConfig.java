package cloud.apposs.balance;

import cloud.apposs.util.CharsetUtil;

public final class LbConfig {
	private static final int DEFAULT_CACHE_PEER_STATUS_TIME = 10 * 1000;
	private static final int DEFAULT_PING_INTERVAL = 10 * 1000;
	private static final int DEFAULT_POLLING_INTERVAL = 30 * 1000;
	
	private String name = "default";
	
	private int peerStatusCacheTime = DEFAULT_CACHE_PEER_STATUS_TIME;
	
	/** 是否启动自动PING检测 */
	private boolean autoPing = false;
	/** PING线程是否为守护线程 */
	private boolean pingDaemon = true;
	/** 自动PING检测的间隔时间 */
	private int pingInterval = DEFAULT_PING_INTERVAL;
	
	private boolean pollingDaemon = true;
	private int pollingInterval = DEFAULT_POLLING_INTERVAL;

	private String charset = CharsetUtil.UTF_8.toString();
	
	private final ZooKeeperConfig zkCfg = new ZooKeeperConfig();
	
	private final QConfConfig qconfCfg = new QConfConfig();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPeerStatusCacheTime() {
		return peerStatusCacheTime;
	}

	public void setPeerStatusCacheTime(int peerStatusCacheTime) {
		if (peerStatusCacheTime > 0) {
			this.peerStatusCacheTime = peerStatusCacheTime;
		}
	}

	public boolean isAutoPing() {
		return autoPing;
	}

	public void setAutoPing(boolean autoPing) {
		this.autoPing = autoPing;
	}

	public boolean isPingDaemon() {
		return pingDaemon;
	}

	public void setPingDaemon(boolean pingDaemon) {
		this.pingDaemon = pingDaemon;
	}

	public int getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(int pingInterval) {
		if (pingInterval > 0) {
			this.pingInterval = pingInterval;
		}
	}
	
	public boolean isPollingDaemon() {
		return pollingDaemon;
	}

	public void setPollingDaemon(boolean pollingDaemon) {
		this.pollingDaemon = pollingDaemon;
	}

	public int getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(int pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public ZooKeeperConfig getZkCfg() {
		return zkCfg;
	}

	public QConfConfig getQconfCfg() {
		return qconfCfg;
	}

	public static class ZooKeeperConfig {
		private static final String DEAFAULT_ZK_SERVER = "127.0.0.1:2181";
		private static final int DEAFAULT_ZK_CONNECT_TIMEOUT = 10 * 1000;
		private static final int DEAFAULT_ZK_SESSION_TIMEOUT = 20 * 1000;
		
		private String servers = DEAFAULT_ZK_SERVER;
		
		private int connectTimeout = DEAFAULT_ZK_CONNECT_TIMEOUT;
        private int sessionTimeout = DEAFAULT_ZK_SESSION_TIMEOUT;
		
		private String path = null;

        public String getServers() {
            return servers;
        }

        public void setServers(String servers) {
            this.servers = servers;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getSessionTimeout() {
            return sessionTimeout;
        }

        public void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
	
	public static class QConfConfig {
		private String env = null;

		private String path = null;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }
    }
}
