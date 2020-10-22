package cloud.apposs.netkit.filterchain.http.server;

public final class HttpConstants {
	public static final int DEFAULT_FILE_LIMIT = 1024 * 1024 * 10;
	public static final String DEFAULT_TMP_DIRECTORY = System.getProperty("java.io.tmpdir");
	
	/** 默认HTTP编码解码 */
	public static final String DEFAULT_CHARSET = "utf-8";
	
	/** 回车换行 */
	public static final byte CR = 13;
    public static final byte LF = 10;
    
    /** 最大header行数字节，避免网络恶意发送大包内存挤爆 */
	public static final int MAX_HEADER_LINE = 8192;
	
	/** BOUNDARY开头划线  */
	public static final byte DASH = 45;
	public static final byte[] BOUNDARY_POSTFIX = {DASH, DASH};
	public static final byte[] BOUNDARY_PREFIX = {CR, LF, DASH, DASH};
	public static final byte[] HEADER_SEPARATOR = {CR, LF, CR, LF};
}
