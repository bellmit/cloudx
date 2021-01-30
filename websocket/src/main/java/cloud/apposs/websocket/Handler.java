package cloud.apposs.websocket;

public class Handler {
    /**
     * 请求的匹配主机
     */
    private final String host;

    /**
     * 请求的匹配路径
     */
    private final String path;

    /**
     * Class类
     */
    private final Class<? extends WSHandler> clazz;

    public Handler(String host, String path, Class<? extends WSHandler> clazz) {
        this.host = host;
        this.path = path;
        this.clazz = clazz;
    }

    public Class<? extends WSHandler> getClazz() {
        return clazz;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(128);
        info.append("{");
        info.append("Bean: ").append(clazz.getSimpleName()).append(", ");
        info.append("Host: ").append(host).append(", ");
        info.append("Path: ").append(path);
        info.append("}");
        return info.toString();
    }
}
