package cloud.apposs.netkit.server.http;

import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.server.ServerConfig;

public class HttpServerConfig extends ServerConfig {
    private String charset = HttpConstants.DEFAULT_CHARSET;

    /**
     * 数据传输时（文件上传）临时文件存储文件
     */
    private String directory = HttpConstants.DEFAULT_TMP_DIRECTORY;

    /**
     * 最大接收的文件大小，小于0为不限制，
     * 主要为了保护业务不让用户上传文件过大，默认不限制
     */
    private long maxFileSize = HttpConstants.DEFAULT_MAX_FILE_SIZE;

    /**
     * 是否将HTTP请求的HEADER KEY自动转换成小写，
     * 在查询header数据时直接用小写获取，无需遍历，便于提升性能，
     * 不过转换为小写业务传递的再获取的时候可能会踩坑，视业务特点而定
     */
    private boolean lowerHeaderKey = false;

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isLowerHeaderKey() {
        return lowerHeaderKey;
    }

    public void setLowerHeaderKey(boolean lowerHeaderKey) {
        this.lowerHeaderKey = lowerHeaderKey;
    }
}
