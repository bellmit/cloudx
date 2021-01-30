package cloud.apposs.registry;

import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.SysUtil;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.Charset;

/**
 * 服务实例注册到zookeeper中，采用netflix curator zookeeper客户端，
 * zookeeper数据结构如下：
 * <pre>/registry/{serviceid}/{host:port}/{ServiceInstance}</pre>
 */
public class CuratorRegistry implements IRegistry {
    private static final String REGISTRY_PATH = "/registry";
    private static final int ZK_CONNECT_TIMEOUT = 10000;
    private static final int ZK_SESSION_TIMEOUT = 60000;

    private CuratorFramework zkClient;

    private Charset charset;

    public CuratorRegistry(String zkServers) {
        this(zkServers, ZK_CONNECT_TIMEOUT, ZK_SESSION_TIMEOUT, CharsetUtil.UTF_8);
    }

    public CuratorRegistry(String zkServers, int connectTimeout, int sessionTimeout) {
        this(zkServers, connectTimeout, sessionTimeout, CharsetUtil.UTF_8);
    }

    public CuratorRegistry(String zkServers, Charset charset) {
        this(zkServers, ZK_CONNECT_TIMEOUT, ZK_SESSION_TIMEOUT, charset);
    }

    public CuratorRegistry(String zkServers, int connectTimeout, int sessionTimeout, Charset charset) {
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
    public boolean registInstance(ServiceInstance serviceInstance) throws Exception {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        String serviceId = serviceInstance.getId();
        String servicePath = REGISTRY_PATH + "/" + serviceId;
        String addressPath = servicePath + "/" + serviceInstance.getPath();
        // 会话地址节点存在时就不重复添加了，避免重复调用
        if (zkClient.checkExists().forPath(addressPath) != null) {
            return false;
        }
        // 创建会话地址节点，连接断开时zookeeper也会自动删除该临时节点
        String serviceValue = serviceInstance.getValue();
        zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).
                forPath(addressPath, serviceValue.getBytes(charset));
        return true;
    }

    @Override
    public boolean deregistInstance(ServiceInstance serviceInstance) throws Exception {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        String serviceId = serviceInstance.getId();
        String servicePath = REGISTRY_PATH + "/" + serviceId;
        String addressPath = servicePath + "/" + serviceInstance.getPath();
        // 会话地址节点已经不存在
        if (zkClient.checkExists().forPath(addressPath) == null) {
            return false;
        }

        // 删除指定节点，示例/registry/{serviceid}/{host:port}
        zkClient.delete().guaranteed().deletingChildrenIfNeeded().forPath(addressPath);
        return true;
    }

    @Override
    public void release() {
        if (zkClient != null) {
            zkClient.close();
            zkClient = null;
        }
    }
}
