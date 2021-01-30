package cloud.apposs.websocket.sample;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.websocket.WSHandlerAdapter;
import cloud.apposs.websocket.annotation.ServerEndpoint;

@ServerEndpoint("/product")
public class ProductEndpoint extends WSHandlerAdapter {
    @Override
    public void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception {
        session.write("Hello from product: " + frame.getText());
    }
}
