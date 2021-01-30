package cloud.apposs.discovery;

import cloud.apposs.balance.IPing;
import cloud.apposs.balance.IRule;
import cloud.apposs.registry.ServiceInstance;

/**
 * 服务发现接口组件，负责维护serviceId:ILoadBalancer的映射关系
 */
public interface IDiscovery {
    /**
     * 选举负载均衡服务节点
     *
     * @param serviceId 服务注册ID
     */
    ServiceInstance choose(String serviceId);

    /**
     * 选举负载均衡服务节点
     *
     * @param serviceId 服务注册ID
     * @param key 负载KEY，例如AID等
     */
    ServiceInstance choose(String serviceId, Object key);

    /**
     * 设置负载均衡模式
     */
    void setRule(String serviceId, IRule rule);

    /**
     * 设置存活检测策略
     */
    void setPing(String serviceId, IPing ping);

    void start() throws Exception;

    boolean shutdown();
}
