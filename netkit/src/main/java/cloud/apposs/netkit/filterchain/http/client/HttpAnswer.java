package cloud.apposs.netkit.filterchain.http.client;

import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.util.CachedFileStream;
import cloud.apposs.util.CharsetUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端网络请求后的数据响应，包括响应状态码、Headers和响应体，
 * 数据体底层采用{@link CachedFileStream}，在读取大文件数据时如果超过一定大小则采用临时文件存储网络数据提升JVM性能
 */
public class HttpAnswer {
    private final String url;

    private final Status status = new Status();

    private final Map<String, String> headers = new HashMap<String, String>();

    private final CachedFileStream buffer;

    public HttpAnswer(String url) throws IOException {
        this(url, null);
    }

    public HttpAnswer(String url, File directory) throws IOException {
        this.url = url;
        this.buffer = new CachedFileStream(HttpConstants.DEFAULT_FILE_LIMIT, directory);
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return getHeader(key, false);
    }

    public String getHeader(String key, boolean ignoreCase) {
        if (ignoreCase) {
            for (String k : headers.keySet()) {
                if (k.equalsIgnoreCase(key)) {
                    return headers.get(k);
                }
            }
        }
        return headers.get(key);
    }

    public void putHeader(String key, String value) {
        headers.put(key, value);
    }

    public void write(byte[] content) throws IOException {
        buffer.write(content);
    }

    public Integer getStatus() {
        return status.getCode();
    }

    public Status getRawStatus() {
        return status;
    }

    public String getContent() throws IOException {
        return getContent(CharsetUtil.UTF_8);
    }

    public String getContent(Charset charset) throws IOException {
        return new String(buffer.array(), charset);
    }

    public byte[] getBytes() throws IOException {
        return buffer.array();
    }

    public CachedFileStream getBuffer() throws IOException {
        return buffer;
    }

    /**
     * 将表单文件数据零拷贝到指定文件
     */
    public long transfer(File dst) throws IOException {
        return buffer.transfer(dst);
    }

    /**
     * 将表单文件数据重命名
     */
    public long rename(File dst) throws IOException {
        return buffer.rename(dst);
    }

    /**
     * HTTP状态码
     */
    public static class Status {
        private String version;

        private Integer code;

        private String description;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
