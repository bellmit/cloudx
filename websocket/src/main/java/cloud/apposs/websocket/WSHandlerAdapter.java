package cloud.apposs.websocket;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;

public class WSHandlerAdapter implements WSHandler {
    @Override
    public boolean onOpen(WebSocketSession session) throws Exception {
        return true;
    }

    @Override
    public void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception {
    }

    @Override
    public void onClose(WebSocketSession session) {
    }

    @Override
    public boolean onError(WebSocketSession session, Throwable cause) throws Exception {
        return true;
    }
}
