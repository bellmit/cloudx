package cloud.apposs.balance;

import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

/**
 * 负载均衡后端服务封装，主要维护IP和端口
 */
public final class Peer {
    /**
     * 后端ID，必须保证该值不为空且唯一
     * 有可能是HOST:PORT，也有可能是URL
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

    private final String url;

    /**
     * 服务是否可连通
     */
    private volatile boolean alive = false;

    /**
     * 服务是否可接入
     */
    private volatile boolean ready = true;

    /**
     * 节点信息元数据，
     * 一般用于节点数据信息扩展，例如是否为主/从节点，节点离线状态等
     */
    private final Param metadata = new Param();

    public Peer(String host, int port) {
        this(host + ":" + port, host, port, host + ":" + port);
    }

    public Peer(String id, String host, int port, String url) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.url = url;
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

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Param getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        if (!StrUtil.isEmpty(key) && value != null) {
            metadata.put(key, value);
        }
    }

    public void addMetadata(Param metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Peer)) {
            return false;
        }
        Peer peer = (Peer) obj;
        return peer.getId().equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
