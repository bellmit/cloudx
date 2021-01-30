package cloud.apposs.balance.balancer;

import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.discovery.PollingDiscovery.DiscoveryAction;
import net.qihoo.qconf.Qconf;

import java.util.LinkedList;
import java.util.List;

/**
 * 基于QConf定时读取更新的负载均衡
 */
public class QconfAwareLoadBalancer extends DynamicLoadBalancer {
    public QconfAwareLoadBalancer(String zkEnv, String zkPath, QconfAwarePeerParser parser) {
        this(generateLbConfig(zkEnv, zkPath), parser);
    }

    public QconfAwareLoadBalancer(LbConfig config, QconfAwarePeerParser parser) {
        super(config, new QconfAwareDiscoveryAction(config.getQconfCfg().getEnv(), config.getQconfCfg().getPath(), parser));
    }

    private static LbConfig generateLbConfig(String zkEnv, String zkPath) {
        LbConfig config = new LbConfig();
        config.getQconfCfg().setEnv(zkEnv);
        config.getQconfCfg().setPath(zkPath);
        return config;
    }

    static class QconfAwareDiscoveryAction implements DiscoveryAction {
        private final String zkEnv;

        private final String zkPath;

        private final QconfAwarePeerParser parser;

        public QconfAwareDiscoveryAction(String zkEnv, String zkPath, QconfAwarePeerParser parser) {
            this.zkEnv = zkEnv;
            this.zkPath = zkPath;
            this.parser = parser;
        }

        @Override
        public void discover(ILoadBalancer balancer) throws Exception {
            List<String> addressPathList = Qconf.getBatchKeys(zkPath, zkEnv);
            List<String> addressList = new LinkedList<String>();
            for (String addressStr : addressPathList) {
                addressList.add((Qconf.getConf(zkPath + "/" + addressStr, zkEnv)));
            }
            List<Peer> allPeers = parser.parseServerList(addressList);
            if (allPeers != null) {
                balancer.updatePeers(allPeers);
            }
        }

        @Override
        public void cause(Throwable cause) {
            parser.exceptionCaught(cause);
        }
    }

    public interface QconfAwarePeerParser {
        List<Peer> parseServerList(List<String> addressList);

        void exceptionCaught(Throwable cause);
    }
}
