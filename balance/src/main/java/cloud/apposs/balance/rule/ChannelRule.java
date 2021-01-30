package cloud.apposs.balance.rule;

import cloud.apposs.balance.AbstractRule;
import cloud.apposs.balance.Peer;
import cloud.apposs.logger.Logger;
import cloud.apposs.util.Param;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 指定通道连接节点的数负载均衡，
 * 背景：
 * 1、针对不同通道的客户端请求（例如高流量用户的请求、写操作用户的请求，读操作用户的请求）做区分来实现服务隔离的负载均衡
 * 2、各节点服务有状态，无法做到水平扩展并随机负载均衡
 * 场景：
 * 1、写通道服务可以为写服务并且单实例，保证该服务在有状态下依然保证不影响性能
 * 2、读通道服务则为读服务并且多实例，这样当读服务性能不足时可以快速水平扩展
 * 3、高流量通道服务则针对高流量用户请求，专门做隔离请求不影响其他用户
 * 规则：
 * 1、Peer节点配置示例：
 * <pre>
 *     {
 *         "ip": "106.75.177.38",
 *         "port": 14080,
 *         "metadata": {
 *             "traffic": true
 *         }
 *     },
 *     {
 *         "ip": "106.75.136.85",
 *         "port": 14080,
 *         "metadata": {
 *             "master": true
 *         }
 *     },
 *     {
 *         "ip": "106.75.136.86",
 *         "port": 14080,
 *         "metadata": {
 *             "slave": true
 *         }
 *     },
 *     {
 *         "ip": "106.75.136.87",
 *         "port": 14080,
 *         "metadata": {
 *             "traffic": true,
 *             "slave": true
 *         }
 *     }
 * </pre>
 * 2、choosePeer(Key)中的Key必须实现IPeerChannel接口以便于判断哪些业务Key是要哪个通道服务
 * 3、Key值可以在业务逻辑中判断该请求Key是属于哪个通道再做通道请求负载均衡
 */
public class ChannelRule extends AbstractRule {
    private final Random random = new Random();

	@Override
	public Peer choosePeer(Object key) {
        if (balancer == null) {
            Logger.warn("No Load Nalancer");
            return null;
        }
        if (!(key instanceof IPeerChannel)) {
            Logger.warn("Key '" + key + "' not implements IPeerChannel");
            return null;
        }

        IPeerChannel peerChannel = (IPeerChannel) key;
        List<Peer> matchedPeerList = new LinkedList<Peer>();
        List<Peer> upList = balancer.getReachablePeer();
        String keyChannel = peerChannel.getChannel();
        // 筛选请求key所在通道的节点列表
        for (Peer peer : upList) {
            Param metadata = peer.getMetadata();
            boolean matched = metadata.getBoolean(keyChannel, false);
            if (matched) {
                matchedPeerList.add(peer);
            }
        }
        if (matchedPeerList.isEmpty()) {
            Logger.warn("No matched peer of channel " + keyChannel);
            return null;
        }
        // 从匹配的通道节点中随机获取可用节点
        int index = random.nextInt(matchedPeerList.size());
        Peer peer = matchedPeerList.get(index);
        return peer;
    }
	
	public interface IPeerChannel {
	    String getChannel();
    }
}
