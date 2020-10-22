package cloud.apposs.util;

import java.nio.ByteBuffer;

/**
 * 数据转换器
 */
public final class Convertor {
    /**
     * 将16进制字符串转换成字节码
     */
    public static byte[] hex2bytes(String hex) {
        int length = hex.length();
        if (length <= 0 || (length % 2) != 0) {
            return null;
        }
        byte[] bytes = new byte[length / 2];
        String value = null;
        for (int i = 0; i < length; i += 2) {
            value = hex.substring(i, i + 2);
            bytes[i / 2] = (byte) Integer.parseInt(value, 16);
        }
        return bytes;
    }

    /**
     * 将字节码转换成16进制字符串
     */
    public static String bytes2hex(byte[] bytes) {
        int length = bytes.length;
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length; ++i) {
            String value = Integer.toHexString(bytes[i] & 0xFF);
            if (value.length() < 2) {
                value = "0" + value;
            } else {
                value = value.substring(value.length() - 2);
            }
            sb.append(value);
        }
        return sb.toString();
    }

    public static ByteBuffer hex2buffer(String hex) {
        return ByteBuffer.wrap(hex2bytes(hex));
    }

    public static String buffer2hex(ByteBuffer buffer) {
        return bytes2hex(buffer.array());
    }
}
