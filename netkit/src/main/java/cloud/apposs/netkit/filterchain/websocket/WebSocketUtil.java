package cloud.apposs.netkit.filterchain.websocket;

import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.util.Base64;
import cloud.apposs.util.StrUtil;

import java.security.MessageDigest;

public final class WebSocketUtil {
    public static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /**
     * 生成服务端WS响应数据，数据响应格式一般如下：
     * <pre>
     *     HTTP/1.1 101 Switching Protocols
     *     Server: Http RxIo Server/1.2.0
     *     Connection: Upgrade
     *     Upgrade: WebSocket
     *     Sec-WebSocket-Accept: FCKgUr8c7OsDsLFeJTWrJw6WO8Q=
     * </pre>
     */
    public static String generateServerResponse(WebSocketSession session) throws Exception {
        String webSocketKey = session.getHeader("Sec-WebSocket-Key");
        if (StrUtil.isEmpty(webSocketKey)) {
            throw new IllegalArgumentException("Sec-WebSocket-Key required");
        }

        String websocketAccept = webSocketKey + MAGIC;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(websocketAccept.getBytes(session.getCharset()), 0, websocketAccept.length());
        byte[] sha1Hash = md.digest();

        StringBuilder response = new StringBuilder(64);
        response.append("HTTP/1.1 101 Switching Protocols\r\n");
        response.append("Server: " + HttpConstants.HTTP_SERVER_NAME + "\r\n");
        response.append("Connection: Upgrade\r\n");
        response.append("Upgrade: WebSocket\r\n");
        response.append("Sec-WebSocket-Accept:").append(Base64.encodeBytes(sha1Hash)).append("\r\n\r\n");
        return response.toString();
    }

    /**
     * 数据帧头部包装
     *
     * @param length 数据包实际长度
     * @param opcode 数制帧的类型
     */
    public static byte[] generateServerHeader(long length, byte opcode) {
        // 掩码开始位置
        int maskingKeyStartIndex = 2;
        // 计算掩码开始位置
        if (length <= 125) {
            maskingKeyStartIndex = 2;
        } else if (length <= 0xFFFF) {
            maskingKeyStartIndex = 4;
        } else {
            maskingKeyStartIndex = 10;
        }
        byte[] header = new byte[maskingKeyStartIndex];
        header[0] = (byte) (0x80 | opcode % 128);
        if (length <= 125) {
            header[1] = (byte) (length);
        } else if (length <= 0xFFFF) {
            header[1] = 0x7E;
            header[2] = (byte) (length >> 8);
            header[3] = (byte) (length & 0xFF);
        } else {
            header[1] = 0x7F;
            header[2] = 0;
            header[3] = 0;
            header[4] = 0;
            header[5] = 0;
            header[6] = (byte) (length >> 24 & 0xFF);
            header[7] = (byte) (length >> 16 & 0xFF);
            header[8] = (byte) (length >> 8 & 0xFF);
            header[9] = (byte) (length & 0xFF);
        }
        return header;
    }
}
