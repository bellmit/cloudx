package cloud.apposs.discovery;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPing;
import cloud.apposs.balance.IRule;
import cloud.apposs.balance.Peer;
import cloud.apposs.registry.ServiceInstance;
import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于ZooKeeper的服务发现负载均衡
 */
public class ZooKeeperDiscovery extends AbstractDiscovery {
    private static final int DEAFAULT_ZK_CONNECT_TIMEOUT = 10 * 1000;
    private static final int DEAFAULT_ZK_SESSION_TIMEOUT = 20 * 1000;

    private final String path;

    private final CuratorFramework zkClient;

    private final Charset charset;

    public ZooKeeperDiscovery(String zkServers, String path) {
        this(zkServers, DEAFAULT_ZK_CONNECT_TIMEOUT, DEAFAULT_ZK_SESSION_TIMEOUT, path, CharsetUtil.UTF_8);
    }

    public ZooKeeperDiscovery(String zkServers, int connectTimeout, int sessionTimeout, String path, Charset charset) {
        this.path = path;
        this.charset = charset;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkServers)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectTimeout)
                .retryPolicy(retryPolicy)
                .build();
        this.zkClient.start();
    }

    @Override
    public void updateLoadBalances(Map<String, ILoadBalancer> balances) throws Exception {
        if (zkClient.checkExists().forPath(path) != null) {
            List<String> serviceIdList = zkClient.getChildren().forPath(path);
            // 更新存在的服务节点数据
            for (String serviceId : serviceIdList) {
                ILoadBalancer balancer = balances.get(serviceId);
                if (balancer == null) {
                    balancer = new BaseLoadBalancer();
                    IRule rule = rules.get(serviceId);
                    if (rule != null) {
                        balancer.setRule(rule);
                    }
                    IPing ping = pings.get(serviceId);
                    if (ping != null) {
                        balancer.setPing(ping);
                    }
                    balances.put(serviceId, balancer);
                }
                String serviceIdPath = path + "/" + serviceId;
                List<String> addressList = zkClient.getChildren().forPath(serviceIdPath);
                List<Peer> peerList = new LinkedList<Peer>();
                for (String address : addressList) {
                    String addressPath = serviceIdPath + "/" + address;
                    String addressStr = new String(zkClient.getData().forPath(addressPath), charset);
                    Param addressInfo = JsonUtil.parseJsonParam(addressStr);
                    String instanceId = addressInfo.getString(ServiceInstance.Name.ID);
                    String instanceHost = addressInfo.getString(ServiceInstance.Name.HOST);
                    int instancePort = addressInfo.getInt(ServiceInstance.Name.PORT);
                    String instanceUrl = addressInfo.getString(ServiceInstance.Name.URL);
                    Peer peer = new Peer(instanceId, instanceHost, instancePort, instanceUrl);
                    peer.addMetadata(addressInfo.getParam(ServiceInstance.Name.METADATA));
                    peerList.add(peer);
                }
                balancer.updatePeers(peerList);
            }

            // 踢除已经不存在的服务节点
            for (String serviceId : balances.keySet()) {
                if (!serviceIdList.contains(serviceId)) {
                    balances.remove(serviceId).shutdown();
                }
            }
        }
    }

    @Override
    public synchronized boolean shutdown() {
        if (super.shutdown()) {
            zkClient.close();
        }
        return false;
    }
}
