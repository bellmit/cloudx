package cloud.apposs.balance.balancer;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPeerDiscovery;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.LbConfig.ZooKeeperConfig;
import cloud.apposs.balance.Peer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.LinkedList;
import java.util.List;

/**
 * ZooKeeper服务自动发现负载均衡，使用ZooKeeper Watcher更新后端服务列表
 */
public class ZooKeeperAwareLoadBalancer extends BaseLoadBalancer {
    public ZooKeeperAwareLoadBalancer(String zkServers, String zkPath, ZooKeeperAwarePeerParser parser) {
        super(generateLbConfig(zkServers, zkPath));
        setDiscovery(new ZooKeeperDiscovery(parser));
    }

	public ZooKeeperAwareLoadBalancer(LbConfig config, ZooKeeperAwarePeerParser parser) {
		super(config);
		setDiscovery(new ZooKeeperDiscovery(parser));
	}

	class ZooKeeperDiscovery implements IPeerDiscovery {
        private CuratorFramework zkClient;

        private ZooKeeperAwarePeerParser parser;

        public ZooKeeperDiscovery(ZooKeeperAwarePeerParser parser) {
            this.parser = parser;
        }

		@Override
		public void start(ILoadBalancer balancer) throws Exception {
            // 初始化zookeeper连接
            doInitZkClient(config.getZkCfg());
            // 初始化服务节点
            List<String> addressList = doInitZkPeers(config);
            List<Peer> allPeers = parser.parseServerList(addressList);
            if (allPeers != null) {
                balancer.addPeers(allPeers);
            }
            // 创建一个服务节点路径监听
            PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, config.getZkCfg().getPath(), true);
            pathChildrenCache.getListenable().addListener(new PeerPathListener());
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        }

        @Override
        public void cause(Throwable cause) {
            parser.exceptionCaught(cause);
        }

        /**
         * 初始化zookeeper连接
         */
		private void doInitZkClient(ZooKeeperConfig config) {
            RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
            this.zkClient = CuratorFrameworkFactory.builder()
                    .connectString(config.getServers())
                    .sessionTimeoutMs(config.getSessionTimeout())
                    .connectionTimeoutMs(config.getConnectTimeout())
                    .retryPolicy(retryPolicy)
                    .build();
            this.zkClient.start();
        }

        /**
         * 从zookeeper中获取可用实例列表
         */
        private List<String> doInitZkPeers(LbConfig config) throws Exception {
            ZooKeeperConfig zkCfg = config.getZkCfg();
            String path = zkCfg.getPath();
            List<String> addressList = new LinkedList<String>();
            if (zkClient.checkExists().forPath(path) != null) {
                List<String> nodeList = zkClient.getChildren().forPath(zkCfg.getPath());
                for (String node : nodeList) {
                    String addressPath = path + "/" + node;
                    addressList.add(new String(zkClient.getData().forPath(addressPath), config.getCharset()));
                }
            }
            return addressList;
        }

        /**
         * 监听zookeer服务节点是否有增加、删除以便于实时更新服务节点
         */
        private class PeerPathListener implements PathChildrenCacheListener {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED ||
                        event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED ||
                        event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                    List<String> addressList = doInitZkPeers(config);
                    List<Peer> allPeers = parser.parseServerList(addressList);
                    if (allPeers != null) {
                        ZooKeeperAwareLoadBalancer.this.updatePeers(allPeers);
                    }
                }
            }
        }

		@Override
		public void shutdown() {
			if (zkClient != null) {
                zkClient.close();
                zkClient = null;
			}
		}
	}

    private static LbConfig generateLbConfig(String zkServers, String zkPath) {
        LbConfig config = new LbConfig();
        config.getZkCfg().setServers(zkServers);
        config.getZkCfg().setPath(zkPath);
        return config;
    }

    public interface ZooKeeperAwarePeerParser {
        List<Peer> parseServerList(List<String> addressList);

        void exceptionCaught(Throwable cause);
    }
}
