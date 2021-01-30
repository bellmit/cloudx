package cloud.apposs.registry;

import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.SysUtil;
import org.I0Itec.zkclient.ZkClient;

import java.nio.charset.Charset;

/**
 * 服务实例注册到zookeeper中，实现分布式的服务注册中心，采用zkclient zookeeper客户端实现，
 * zookeeper数据结构如下：
 * <pre>/registry/{serviceid}/{host:port}/{ServiceInstance}</pre>
 * 参考：
 * <pre>
 * https://blog.csdn.net/jiaowo_ccc/article/details/87976422
 * https://www.cnblogs.com/bearduncle/p/8602554.html
 * </pre>
 */
public class ZookeeperRegistry implements IRegistry {
    private static final String REGISTRY_PATH = "/registry";
    private static final int ZK_CONNECT_TIMEOUT = 10000;
    private static final int ZK_SESSION_TIMEOUT = 60000;

    private ZkClient zkClient;

    private Charset charset;

    public ZookeeperRegistry(String zkServers) {
        this(zkServers, ZK_CONNECT_TIMEOUT, ZK_SESSION_TIMEOUT, CharsetUtil.UTF_8);
    }

    public ZookeeperRegistry(String zkServers, int connectTimeout, int sessionTimeout) {
        this(zkServers, connectTimeout, sessionTimeout, CharsetUtil.UTF_8);
    }

    public ZookeeperRegistry(String zkServers, Charset charset) {
        this(zkServers, ZK_CONNECT_TIMEOUT, ZK_SESSION_TIMEOUT, charset);
    }

    public ZookeeperRegistry(String zkServers, int connectTimeout, int sessionTimeout, Charset charset) {
        this.charset = charset;
        this.zkClient = new ZkClient(zkServers, connectTimeout, sessionTimeout);
    }

    @Override
    public boolean registInstance(ServiceInstance serviceInstance) {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        // 根节点不存在时先创建根节点（持久节点）
        if (!zkClient.exists(REGISTRY_PATH)) {
            zkClient.createPersistent(REGISTRY_PATH);
        }
        // 创建服务节点（持久节点）
        String serviceId = serviceInstance.getId();
        String servicePath = REGISTRY_PATH + "/" + serviceId;
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }
        // 会话地址节点存在则不创建，避免重复创建
        String addressPath = servicePath + "/" + serviceInstance.getPath();
        if (zkClient.exists(addressPath)) {
            return false;
        }
        // 创建会话地址节点，连接断开时zookeeper也会自动删除该临时节点
        String serviceValue = serviceInstance.getValue();
        zkClient.createEphemeral(addressPath, serviceValue.getBytes(charset));
        return true;
    }

    @Override
    public boolean deregistInstance(ServiceInstance serviceInstance) {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        // 根节点不存在
        if (!zkClient.exists(REGISTRY_PATH)) {
            return false;
        }
        // 服务节点不存在
        String serviceId = serviceInstance.getId();
        String servicePath = REGISTRY_PATH + "/" + serviceId;
        if (!zkClient.exists(servicePath)) {
            return false;
        }
        // 删除指定节点，示例/registry/{serviceid}/{host:port}
        String addressPath = servicePath + "/" + serviceInstance.getPath();
        if (!zkClient.exists(addressPath)) {
            return false;
        }
        return zkClient.delete(addressPath);
    }

    @Override
    public void release() {
        if (zkClient != null) {
            zkClient.close();
            zkClient = null;
        }
    }
}
