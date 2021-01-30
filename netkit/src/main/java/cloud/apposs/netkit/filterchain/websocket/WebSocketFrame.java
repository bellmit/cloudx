package cloud.apposs.netkit.filterchain.websocket;

import java.nio.charset.Charset;

/**
 * WebSocket数据帧包装，当接收到一个完整的数据帧时才传递到后端
 */
public class WebSocketFrame {
    /**
     * OPCODE类型
     */
    public static final byte OPCODE_CONT = 0x0;
    public static final byte OPCODE_TEXT = 0x1;
    public static final byte OPCODE_BINARY = 0x2;
    public static final byte OPCODE_CLOSE = 0x8;
    public static final byte OPCODE_PING = 0x9;
    public static final byte OPCODE_PONG = 0xA;

    /**
     * 当前数据帧是否已经结束
     */
    private final boolean finalFragment;

    private final int opcode;

    private final String charset;

    /**
     * 接收的数据体
     */
    private byte[] buffer;

    public WebSocketFrame(boolean finalFragment, int opcode, String charset) {
        this.finalFragment = finalFragment;
        this.opcode = opcode;
        this.charset = charset;
    }

    public boolean isFinalFragment() {
        return finalFragment;
    }

    public int getOpcode() {
        return opcode;
    }

    public void allocate(int length) {
        buffer = new byte[length];
    }

    public byte[] buffer() {
        return buffer;
    }

    public String getText() {
        return getText(Charset.forName(charset));
    }

    public String getText(Charset charset) {
        if (buffer == null) {
            return null;
        }
        return new String(buffer, charset);
    }

    void write(byte[] buffer, int offset, int length) {
        System.arraycopy(buffer, 0, this.buffer, offset, length);
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(32);
        if (opcode == OPCODE_CONT) {
            info.append("OPCODE_CONT");
        } else if (opcode == OPCODE_TEXT) {
            info.append("OPCODE_TEXT");
        } else if (opcode == OPCODE_BINARY) {
            info.append("OPCODE_BINARY");
        } else if (opcode == OPCODE_CLOSE) {
            info.append("OPCODE_CLOSE");
        } else if (opcode == OPCODE_PING) {
            info.append("OPCODE_PING");
        } else if (opcode == OPCODE_PONG) {
            info.append("OPCODE_PONG");
        }
        return info.toString();
    }
}
