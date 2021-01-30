package cloud.apposs.discovery;

import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPing;
import cloud.apposs.balance.IRule;
import cloud.apposs.balance.Peer;
import cloud.apposs.registry.ServiceInstance;
import cloud.apposs.util.StrUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDiscovery implements IDiscovery {
    /**
     * 服务注册ID:负载均衡映射
     */
    protected Map<String, ILoadBalancer> balances;

    /**
     * 服务主动发现轮询器，负责定时检查服务注册ID对应的实例列表是否有更新
     */
    protected PollingServiceDiscovery polling;

    protected AtomicBoolean active = new AtomicBoolean(false);

    /**
     * 负载均衡规则，可定义每个服务注册ID不同的负载均衡模式
     */
    protected final Map<String, IRule> rules = new HashMap<String, IRule>();

    /**
     * 是否定时检查服务是否可用，可定义每个服务注册ID不同的存活检测模式
     */
    protected final Map<String, IPing> pings = new HashMap<String, IPing>();

    public AbstractDiscovery() {
        this(true);
    }

    public AbstractDiscovery(boolean autoPolling) {
        this.balances = new ConcurrentHashMap<String, ILoadBalancer>();
        if (autoPolling) {
            this.polling = new PollingServiceDiscovery();
        }
    }

    @Override
    public ServiceInstance choose(String serviceId) {
        return choose(serviceId, "default");
    }

    @Override
    public ServiceInstance choose(String serviceId, Object key) {
        ILoadBalancer balancer = balances.get(serviceId);
        if (balancer == null) {
            return null;
        }
        Peer peer = balancer.choosePeer(key);
        if (peer == null) {
            return null;
        }
        return new ServiceInstance(peer.getId(), peer.getHost(), peer.getPort(), peer.getUrl(), peer.getMetadata());
    }

    @Override
    public void setRule(String serviceId, IRule rule) {
        if (StrUtil.isEmpty(serviceId) || rule == null) {
            throw new IllegalArgumentException();
        }
        rules.put(serviceId, rule);
        ILoadBalancer balancer = balances.get(serviceId);
        if (balancer != null) {
            balancer.setRule(rule);
        }
    }

    @Override
    public void setPing(String serviceId, IPing ping) {
        if (StrUtil.isEmpty(serviceId) || ping == null) {
            throw new IllegalArgumentException();
        }
        pings.put(serviceId, ping);
        ILoadBalancer balancer = balances.get(serviceId);
        if (balancer != null) {
            balancer.setPing(ping);
        }
    }

    public abstract void updateLoadBalances(Map<String, ILoadBalancer> balances) throws Exception;

    @Override
    public synchronized void start() throws Exception {
        if (!active.compareAndSet(false, true)) {
            return;
        }
        updateLoadBalances(balances);
        if (polling != null) {
            polling.start();
        }
    }

    @Override
    public synchronized boolean shutdown() {
        return active.compareAndSet(true, false);
    }

    /**
     * 定期检测是否有新注册服务节点
     */
    private class PollingServiceDiscovery extends Thread {
        private static final String THREAD_NAME = "Thread-PollingServiceDiscovery";
        private static final int DEFAULT_INTERVAL = 30 * 1000;

        private int interval = DEFAULT_INTERVAL;

        public PollingServiceDiscovery() {
            this.setName(THREAD_NAME);
            this.setDaemon(true);
        }

        @Override
        public void run() {
            while (active.get()) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                }
                try {
                    updateLoadBalances(balances);
                } catch (Exception e) {
                }
            }
        }
    }
}
