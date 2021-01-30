package cloud.apposs.websocket.sample;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.websocket.WSHandlerAdapter;
import cloud.apposs.websocket.annotation.ServerEndpoint;

@ServerEndpoint("/user")
public class UserEndpoint extends WSHandlerAdapter {
    @Override
    public boolean onOpen(WebSocketSession session) {
        System.out.println(session + " connected");
        return true;
    }

    @Override
    public void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception {
        session.write("Hello from server: " + frame.getText());
    }

    @Override
    public void onClose(WebSocketSession session) {
        System.out.println(session + " closed");
    }

    @Override
    public boolean onError(WebSocketSession session, Throwable cause) throws Exception {
        System.out.println("Session fail: " + cause.getMessage());
        return true;
    }
}
