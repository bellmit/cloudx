package cloud.apposs.discovery;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.Peer;

import java.util.List;
import java.util.Map;

/**
 * 基于内存的服务发现负载均衡
 */
public class MemoryDiscovery extends AbstractDiscovery {
    /**
     * 内存构造时直接初始化
     *
     * @param peers 服务列表
     */
    public MemoryDiscovery(Map<String, List<Peer>> peers) {
        super(false);
        for (String serviceId : peers.keySet()) {
            List<Peer> peerList = peers.get(serviceId);
            LbConfig lbConfig = new LbConfig();
            lbConfig.setAutoPing(false);
            ILoadBalancer balancer = new BaseLoadBalancer(lbConfig);
            balancer.addPeers(peerList);
            balances.put(serviceId, balancer);
        }
    }

    @Override
    public void updateLoadBalances(Map<String, ILoadBalancer> balances) throws Exception {
        // 因为是直接内存加载，没有所谓的节点更新
    }
}
