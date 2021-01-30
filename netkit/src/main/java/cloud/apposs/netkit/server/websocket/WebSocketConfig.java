package cloud.apposs.netkit.server.websocket;

import cloud.apposs.netkit.server.ServerConfig;

public class WebSocketConfig extends ServerConfig {
    /**
     * 是否将WS请求的HEADER KEY自动转换成小写，
     * 在查询header数据时直接用小写获取，无需遍历，便于提升性能，
     * 不过转换为小写业务传递的再获取的时候可能会踩坑，视业务特点而定
     */
    private boolean lowerHeaderKey = false;

    public boolean isLowerHeaderKey() {
        return lowerHeaderKey;
    }

    public void setLowerHeaderKey(boolean lowerHeaderKey) {
        this.lowerHeaderKey = lowerHeaderKey;
    }
}
