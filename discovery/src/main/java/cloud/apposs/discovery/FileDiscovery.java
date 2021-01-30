package cloud.apposs.discovery;

import cloud.apposs.balance.BaseLoadBalancer;
import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPing;
import cloud.apposs.balance.IRule;
import cloud.apposs.balance.Peer;
import cloud.apposs.util.FileUtil;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于配置文件读取的服务发现负载均衡，配置文件格式如下：
 * <pre>
 * {
 *     "proxy_socks": [
 *         {
 *             "ip": "106.75.177.38",
 *             "port": 14080
 *         },
 *         {
 *             "ip": "106.75.136.85",
 *             "port": 14080
 *         }
 *     ],
 *     "proxy_domain": [
 *         {
 *             "ip": "106.75.148.110",
 *             "port": 14080
 *         }
 *     ]
 * }
 * </pre>
 */
public class FileDiscovery extends AbstractDiscovery {
    public static final String IP = "ip";
    public static final String PORT = "port";
    public static final String METADATA = "metadata";

    private File cachefile;

    /** 文件修改时间 ，主要用于判断文件配置是否更新 */
    private long lastModified = 0;

    public FileDiscovery(String filename) throws IOException {
        File config = new File(filename);
        if (!config.exists()) {
            throw new FileNotFoundException(filename);
        }
        this.cachefile = config;
    }

    @Override
    public void updateLoadBalances(Map<String, ILoadBalancer> balances) throws Exception {
        long fileLastModified = cachefile.lastModified();
        if (fileLastModified == lastModified) {
            return;
        }

        String json = FileUtil.readString(cachefile);
        if (StrUtil.isEmpty(json)) {
            return;
        }
        Param config = JsonUtil.parseJsonParam(json);
        if (config == null) {
            return;
        }
        lastModified = fileLastModified;
        for (String serviceId : config.keySet()) {
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

            List<Param> addressList = config.getList(serviceId);
            List<Peer> peerList = new LinkedList<Peer>();
            for (Param addressInfo : addressList) {
                String instanceHost = addressInfo.getString(IP);
                int instancePort = addressInfo.getInt(PORT);
                Peer peer = new Peer(instanceHost, instancePort);
                peer.addMetadata(addressInfo.getParam(METADATA));
                peerList.add(peer);
            }
            balancer.updatePeers(peerList);
        }
    }
}
