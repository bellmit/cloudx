package cloud.apposs.websocket;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;

public interface WSHandler {
    /**
     * 建立连接
     */
    boolean onOpen(WebSocketSession session) throws Exception;

    /**
     * 数据接收
     *
     * @param session 请求会议，负责数据发送
     * @param frame 请求数据帧，长连接每个请求对应每个数据帧
     */
    void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception;

    /**
     * 会话关闭
     */
    void onClose(WebSocketSession session);

    /**
     * 会话异常，主要服务于降级
     *
     * @return true则直接告诉底层把包扔掉，false则自己处理这些包并发送给客户端，不做任何降级实现默认返回-1错误码给客户端
     */
    boolean onError(WebSocketSession session, Throwable cause) throws Exception;
}
