package cloud.apposs.netkit.server.http;

import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.server.ServerConfig;

public class HttpServerConfig extends ServerConfig {
    private String charset = HttpConstants.DEFAULT_CHARSET;

    private String directory = HttpConstants.DEFAULT_TMP_DIRECTORY;

    /**
     * 是否将HTTP请求的HEADER KEY自动转换成小写，便于提升性能
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

    public boolean isLowerHeaderKey() {
        return lowerHeaderKey;
    }

    public void setLowerHeaderKey(boolean lowerHeaderKey) {
        this.lowerHeaderKey = lowerHeaderKey;
    }
}
