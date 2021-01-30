package cloud.apposs.netkit.filterchain.http.server;

public final class HttpConstants {
    public static final int DEFAULT_FILE_LIMIT = 1024 * 1024 * 10;
    /**
     * HTTP协议版本
     */
    public static final float HTTP_PROTOCOL_1_0 = 1.0F;
    public static final float HTTP_PROTOCOL_1_1 = 1.1F;

    /**
     * 响应的服务器信息
     */
    public static final String HTTP_SERVER_NAME = "Http RxIo Server/1.2.0";

    /**
     * 数据传输时（文件上传）临时文件存储文件
     */
    public static final String DEFAULT_TMP_DIRECTORY = System.getProperty("java.io.tmpdir");

    /**
     * 最大接收的文件大小，小于0为不限制
     */
    public static final int DEFAULT_MAX_FILE_SIZE = -1;

    /**
     * 默认HTTP编码解码
     */
    public static final String DEFAULT_CHARSET = "utf-8";

    /**
     * 回车换行
     */
    public static final byte CR = 13;
    public static final byte LF = 10;

    /**
     * 最大header行数字节，避免网络恶意发送大包内存挤爆
     */
    public static final int MAX_HEADER_LINE = 8192;

    /**
     * BOUNDARY开头划线
     */
    public static final byte DASH = 45;
    public static final byte[] BOUNDARY_POSTFIX = {DASH, DASH};
    public static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};
    public static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};
}
