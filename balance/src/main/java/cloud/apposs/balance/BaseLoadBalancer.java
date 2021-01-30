package cloud.apposs.balance;

import cloud.apposs.balance.balancer.LoadBalancerStats;
import cloud.apposs.balance.ping.NoOpPing;
import cloud.apposs.balance.rule.RoundRobinRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 负载均衡类的基础基类，
 * 注意ILoadBalancer底层用定时线程轮询查找远程服务可用线路和检测服务存活情况，
 * 所以在短连接对象中要设置成全局单例避免线程资源占用过多
 */
public class BaseLoadBalancer extends AbstractLoadBalancer {
    private final static IPing DEFAULT_PING = new NoOpPing();

    /**
     * 自定义的负载均衡策略
     */
    protected IRule rule = null;

    /**
     * 获取实例时的网络PING检测服务，默认为不做任何操作
     */
    protected IPing ping = DEFAULT_PING;

    /**
     * 自定义的服务发现策略
     */
    private IPeerDiscovery discovery = null;

    protected LoadBalancerStats stats = new LoadBalancerStats();

    protected LbConfig config;

    protected List<Peer> allPeerList = new CopyOnWriteArrayList<Peer>();
    protected List<Peer> upPeerList = new CopyOnWriteArrayList<Peer>();

    protected Pinger pinger;

    protected List<PeerListener> listeners = new CopyOnWriteArrayList<PeerListener>();

    /**
     * 后端节点存活状态的缓存
     */
    private Map<String, PeerStatus> lastPeerStatus = new ConcurrentHashMap<String, PeerStatus>();

    public BaseLoadBalancer() {
        this(new LbConfig());
    }

    public BaseLoadBalancer(LbConfig config) {
        this.config = config;
        this.setRule(new RoundRobinRule());
        if (config.isAutoPing()) {
            this.pinger = new Pinger();
            this.pinger.setDaemon(config.isPingDaemon());
            this.pinger.start();
        }
    }

    @Override
    public void setRule(IRule rule) {
        if (rule != null) {
            this.rule = rule;
            if (this.rule.getLoadBalancer() != this) {
                this.rule.setLoadBalancer(this);
            }
        }
    }

    @Override
    public void setPing(IPing ping) {
        if (ping != null) {
            if (!ping.equals(this.ping)) {
                this.ping = ping;
            }
        }
    }

    public synchronized void setDiscovery(IPeerDiscovery discovery) {
        if (discovery != null && this.discovery == null) {
            this.discovery = discovery;
			try {
                this.discovery.start(this);
            } catch (Exception e) {
			    this.discovery.cause(e);
            }
        }
    }

    public void setConfig(LbConfig config) {
        this.config = config;
    }

    @Override
    public void addPeers(Peer... peers) {
        if (peers == null || peers.length <= 0) {
            return;
        }
        for (int i = 0; i < peers.length; i++) {
            Peer peer = peers[i];
            doAddPeer(peer);
        }
    }

    @Override
    public void addPeers(List<Peer> peers) {
        if (peers == null || peers.isEmpty()) {
            return;
        }
        for (Peer peer : peers) {
            doAddPeer(peer);
        }
    }

    @Override
    public void updatePeers(List<Peer> peers) {
        if (peers == null || peers.isEmpty()) {
            return;
        }
        for (Iterator<Peer> iterator = peers.iterator(); iterator.hasNext(); ) {
            Peer peer = iterator.next();
            if (!peers.contains(peer)) {
                iterator.remove();
                peer.setAlive(false);
                firePeerChanged(peer);
            } else {
                allPeerList.add(peer);
                if (isPeerAlive(peer)) {
                    upPeerList.add(peer);
                }
                firePeerAdded(peer);
            }
        }
    }

    @Override
    public Peer choosePeer(Object key) {
        if (rule == null) {
            return null;
        }

        return rule.choosePeer(key);
    }

    @Override
    public List<Peer> getAllPeers() {
        return allPeerList;
    }

    @Override
    public List<Peer> getReachablePeer() {
        return upPeerList;
    }

    @Override
    public void markPeerDown(Peer peer) {
        if (peer == null || !peer.isAlive()) {
            return;
        }

        peer.setAlive(false);
        firePeerChanged(peer);
    }

    /**
     * 检查后端节点是否存活，
     * 因为在每次调用{@link #choosePeer(Object)}均会触发存活检测，需要缓存结果来提升性能
     */
    @Override
    public boolean isPeerAlive(Peer peer) {
        if (peer == null) {
            return false;
        }

        String id = peer.getId();
        PeerStatus peerStatus = lastPeerStatus.get(id);
        if (peerStatus == null) {
            peerStatus = new PeerStatus();
            lastPeerStatus.put(id, peerStatus);
        }
        boolean isPeerAlive = peerStatus.isAlive();
        long lastUpdateTime = peerStatus.getLastUpdateTime();
        long now = System.currentTimeMillis();
        // 默认缓存1分钟内的查询结果
        if (now - lastUpdateTime < config.getPeerStatusCacheTime()) {
            return isPeerAlive;
        }
        isPeerAlive = ping.isAlive(peer);
        peerStatus.setAlive(isPeerAlive);
        peerStatus.setLastUpdateTime(System.currentTimeMillis());
        return isPeerAlive;
    }

    @Override
    public LoadBalancerStats getLoadBalancerStats() {
        return stats;
    }

    private void doAddPeer(Peer peer) {
        if (!allPeerList.contains(peer)) {
            allPeerList.add(peer);
            if (isPeerAlive(peer)) {
                upPeerList.add(peer);
            }
            firePeerAdded(peer);
        }
    }

    private void firePeerAdded(Peer peer) {
        for (PeerListener listener : listeners) {
            listener.peerAdded(peer);
        }
    }

    private void firePeerChanged(Peer peer) {
        for (PeerListener listener : listeners) {
            listener.peerChanged(peer);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (discovery != null) {
            discovery.shutdown();
        }
        if (pinger != null) {
            pinger.shutdown();
        }
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(48);
        info.append("{LoadBanalcer:");
        info.append("name=" + config.getName() + ",");
        info.append("auto_ping=" + config.isAutoPing() + ",");
        info.append("ping_daemon=" + config.isPingDaemon());
        info.append("}");
        return info.toString();
    }

    static class PeerStatus {
        private boolean alive = false;

        private long lastUpdateTime = 0;

        public boolean isAlive() {
            return alive;
        }

        public void setAlive(boolean alive) {
            this.alive = alive;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }
    }

    class Pinger extends Thread {
        private volatile boolean shutdown = false;

        @Override
        public void run() {
            while (!shutdown) {
                int pingInterval = config.getPingInterval();

                List<Peer> newUpPeerList = new CopyOnWriteArrayList<Peer>();
                int total = allPeerList.size();
                Peer[] allPeers = allPeerList.toArray(new Peer[total]);
                List<Peer> changedServers = new ArrayList<Peer>();

                for (int i = 0; i < total; i++) {
                    Peer peer = allPeers[i];
                    boolean alive = ping.isAlive(peer);
                    boolean oldAlive = peer.isAlive();
                    peer.setAlive(alive);
                    if (oldAlive != alive) {
                        changedServers.add(peer);
                    }

                    if (alive) {
                        newUpPeerList.add(peer);
                    }
                }
                upPeerList = newUpPeerList;
                doNotifyPeerStatusChangeListener(changedServers);

                try {
                    Thread.sleep(pingInterval);
                } catch (InterruptedException e) {
                    if (shutdown) {
                        return;
                    }
                }
            }
        }

        public synchronized void shutdown() {
            if (!shutdown) {
                shutdown = true;
                interrupt();
            }
        }

        private void doNotifyPeerStatusChangeListener(final List<Peer> changedPeers) {
            for (Peer peer : changedPeers) {
                firePeerChanged(peer);
            }
        }
    }
}
