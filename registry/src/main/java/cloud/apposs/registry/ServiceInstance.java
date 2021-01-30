package cloud.apposs.registry;

import cloud.apposs.util.Param;
import cloud.apposs.util.SysUtil;

/**
 * 负载均衡后端服务封装，主要维护IP和端口
 */
public final class ServiceInstance {
    public static final class Name {
        public static final String ID = "id";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String URL = "url";
        public static final String METADATA = "metadata";
    }

    /**
     * 服务ID，必须保证该值不为空，
     * 一个服务ID下面可以有多个服务实例以便于实现微服务负载均衡
     */
    private final String id;

    /**
     * 后端主机IP
     */
    private final String host;

    /**
     * 后端主机端口
     */
    private final int port;

    /**
     * 后端请求URL
     */
    private final String url;

    /**
     * 服务实例附加信息
     */
    private final Param metadata;

    public ServiceInstance(String id, String host, int port) {
        this(id, host, port, host + ":" + port, new Param());
    }

    public ServiceInstance(String id, String host, int port, String url) {
        this(id, host, port, url, new Param());
    }

    public ServiceInstance(String id, String host, int port, String url, Param metadata) {
        SysUtil.checkNotNull(id);
        SysUtil.checkNotNull(host);
        SysUtil.checkNotNull(port);
        SysUtil.checkNotNull(url);
        SysUtil.checkNotNull(metadata);

        this.host = host;
        this.port = port;
        this.id = id;
        this.url = url;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return url;
    }

    public Param getMetadata() {
        return metadata;
    }

    /**
     * 主要用于zookeeper等配置中心注册路径时使用
     */
    public String getPath() {
        return host + ":" + port;
    }

    public String getValue() {
        StringBuilder info = new StringBuilder(64);
        info.append("{");
        info.append("\"" + Name.ID + "\":\"" + id + "\",");
        info.append("\"" + Name.URL + "\":\"" + url + "\",");
        info.append("\"" + Name.HOST + "\":\"" + host + "\",");
        info.append("\"" + Name.PORT + "\":" + port + ",");
        info.append("\"" + Name.METADATA + "\":" + metadata);
        info.append("}");
        return info.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceInstance)) {
            return false;
        }
        ServiceInstance serviceInstance = (ServiceInstance) obj;
        return serviceInstance.getId().equals(id) &&
                serviceInstance.getHost().equals(host) && serviceInstance.getPort() == port;
    }

    @Override
    public int hashCode() {
        return getValue().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(64);
        info.append("ServiceInstance");
        info.append(getValue());
        return info.toString();
    }
}
