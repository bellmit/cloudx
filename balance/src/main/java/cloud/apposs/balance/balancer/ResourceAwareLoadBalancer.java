package cloud.apposs.balance.balancer;

import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.discovery.PollingDiscovery.DiscoveryAction;
import cloud.apposs.logger.Logger;
import cloud.apposs.logger.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 基于ClassPath查找配置文件定时更新的负载均衡
 */
public class ResourceAwareLoadBalancer extends DynamicLoadBalancer {
    public ResourceAwareLoadBalancer(String filename, ResourceAwarePeerParser parser) {
        this(new LbConfig(), filename, parser);
    }

    public ResourceAwareLoadBalancer(LbConfig config, String filename, ResourceAwarePeerParser parser) {
        super(config, new ResourceAwareDiscoveryAction(filename, parser));
    }

    static class ResourceAwareDiscoveryAction implements DiscoveryAction {
        private ResourceAwarePeerParser parser;

        private String filename;

        public ResourceAwareDiscoveryAction(String filename, ResourceAwarePeerParser parser) {
            if (filename == null) {
                throw new IllegalArgumentException("filename");
            }
            if (parser == null) {
                throw new IllegalArgumentException("parser");
            }

            InputStream buffer = null;
            try {
                this.parser = parser;
                this.filename = filename;
                buffer = ResourceUtil.getInputStream(filename);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } finally {
                if (buffer != null) {
                    try {
                        buffer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        @Override
        public void discover(ILoadBalancer balancer) throws Exception {
            InputStream buffer = null;
            try {
                buffer = ResourceUtil.getInputStream(filename);
                List<Peer> allPeers = parser.parseServerList(buffer);
                if (allPeers != null) {
                    balancer.addPeers(allPeers);
                }
            } finally {
                if (buffer != null) {
                    try {
                        buffer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        @Override
        public void cause(Throwable cause) {
            Logger.error(cause, "resource discovery fail");
        }
    }

    public interface ResourceAwarePeerParser {
        List<Peer> parseServerList(InputStream content);
    }
}
