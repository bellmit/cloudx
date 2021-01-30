package cloud.apposs.registry;

/**
 * 服务注册、发现接口
 */
public interface IRegistry {
    /**
     * 注册服务到配置中心以便于客户端发现服务，核心接口，
     * 一用于当微服务启动时将自己注册到配置中心
     *
     * @return 成功注册时返回true
     */
    boolean registInstance(ServiceInstance serviceInstance) throws Exception;

    /**
     * 从配置中心注销该服务以便于客户端剔除该服务，
     * 一般用于当微服务关闭时调用
     *
     * @return 成功注销时返回true
     */
    boolean deregistInstance(ServiceInstance serviceInstance) throws Exception;

    /**
     * 服务退出，释放资源，例如断开zookeeper连接，释放注册资源等
     */
    void release();
}
