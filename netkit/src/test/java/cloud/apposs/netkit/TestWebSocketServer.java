package cloud.apposs.netkit;

import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.netkit.server.websocket.WebSocketConfig;
import cloud.apposs.netkit.server.websocket.WebSocketHandlerAdapter;
import cloud.apposs.netkit.server.websocket.WebSocketServer;

public class TestWebSocketServer {
    public static void main(String[] args) throws Exception {
        WebSocketConfig config = new WebSocketConfig();
        config.setPort(8820);
        config.setRecvTimeout(30 * 60 * 1000);
        WebSocketServer server = new WebSocketServer(config);
        server.setHandler(new WebSocketSimple());
        server.start();
    }

    static class WebSocketSimple extends WebSocketHandlerAdapter {
        @Override
        public boolean onOpen(WebSocketSession session) throws Exception {
            System.out.println("Session accpeted: " + session);
            return true;
        }

        @Override
        public void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception {
            System.out.println(frame.getText());
            session.write("Hello from server: " + frame.getText());
        }

        @Override
        public void onClose(WebSocketSession session) {
            System.out.println("Session close");
        }

        @Override
        public boolean onError(WebSocketSession session, Throwable cause) throws Exception {
            cause.printStackTrace();
            return true;
        }
    }
}
