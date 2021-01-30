package cloud.apposs.discovery;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPing;
import cloud.apposs.balance.IRule;
import cloud.apposs.balance.Peer;
import cloud.apposs.registry.ServiceInstance;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import net.qihoo.qconf.Qconf;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于QConf的服务发现负载均衡
 */
public class QconfDiscovery extends AbstractDiscovery {
    private final String path;

    private final String environment;

    public QconfDiscovery(String environment, String path) {
        this.environment = environment;
        this.path = path;
    }

    @Override
    public void updateLoadBalances(Map<String, ILoadBalancer> balances) throws Exception {
        List<String> serviceIdList = Qconf.getBatchKeys(path, environment);
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
            List<String> addressList = Qconf.getBatchKeys(serviceIdPath, environment);
            List<Peer> peerList = new LinkedList<Peer>();
            for (String address : addressList) {
                String addressPath = serviceIdPath + "/" + address;
                String addressStr = Qconf.getConf(addressPath, environment);
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
