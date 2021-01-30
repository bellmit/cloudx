package cloud.apposs.netkit.server.websocket;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFilter;
import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.netkit.filterchain.websocket.WebSocketUtil;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;

import java.net.SocketTimeoutException;

public abstract class WebSocketHandler extends ServerHandlerAdaptor {
    @Override
    public void channelRead(ServerHandlerContext context, Object message) throws Exception {
        WebSocketSession session = (WebSocketSession) context.getAttribute(WebSocketFilter.FILTER_SESSION_CONTEXT);
        if (session == null) {
            context.close(true);
            return;
        }
        // 参数为Boolean时表示是首次建立连接
        if (message instanceof Boolean) {
            if (onOpen(session)) {
                // 发送服务端响应数据给客户端建立WS连接
                context.write(WebSocketUtil.generateServerResponse(session));
            } else {
                context.close(true);
            }
        } else {
            onMessage(session, (WebSocketFrame) message);
        }
    }

    @Override
    public void channelClose(ServerHandlerContext context) {
        WebSocketSession session = (WebSocketSession) context.getAttribute(WebSocketFilter.FILTER_SESSION_CONTEXT);
        if (session == null) {
            context.close(true);
            return;
        }
        onClose(session);
    }

    @Override
    public void channelError(ServerHandlerContext context, Throwable cause) {
        if (cause instanceof SocketTimeoutException) {
            context.close(true);
            return;
        }
        WebSocketSession session = (WebSocketSession) context.getAttribute(WebSocketFilter.FILTER_SESSION_CONTEXT);
        if (session == null) {
            context.close(true);
            return;
        }

        boolean fallback = true;
        try {
            onError(session, cause);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        session.close(fallback);
    }

    /**
     * 建立连接
     *
     * @return 首次建立连接时如果业务不想处理数据(例如超过最大连接数)可返回false，关闭会话
     */
    public abstract boolean onOpen(WebSocketSession session) throws Exception;

    /**
     * 数据接收
     *
     * @param session 请求会议，负责数据发送
     * @param frame 请求数据帧，长连接每个请求对应每个数据帧
     */
    public abstract void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception;

    /**
     * 会话关闭
     */
    public abstract void onClose(WebSocketSession session);

    /**
     * 会话异常，主要服务于降级
     *
     * @return true则直接告诉底层把包扔掉，false则自己处理这些包并发送给客户端，不做任何降级实现默认返回-1错误码给客户端
     */
    public abstract boolean onError(WebSocketSession session, Throwable cause) throws Exception;
}
