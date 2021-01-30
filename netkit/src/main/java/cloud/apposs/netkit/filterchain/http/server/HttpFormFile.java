package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.util.CachedFileStream;

import java.io.File;
import java.io.IOException;

/**
 * 表单文件数据，
 * 会持续读取HTTP上传的FORM表单文件，当超过一定上传文件大小时会自动改成临时文件存储以减少JVM内存压力
 */
public final class HttpFormFile {
    private final String filename;

    private final CachedFileStream cachefile;

    public HttpFormFile(String filename) {
        this.filename = filename;
        this.cachefile = new CachedFileStream();
    }

    public HttpFormFile(String filename, int threshold, File directory) {
        this.filename = filename;
        this.cachefile = new CachedFileStream(threshold, directory);
    }

    public byte[] getRawData() {
        return cachefile.getRawData();
    }

    public String getFilename() {
        return filename;
    }

    /**
     * 获取表单文件大小
     */
    public long size() {
        return cachefile.size();
    }

    /**
     * 添加文件数据
     */
    public void write(byte[] buffer) throws IOException {
        cachefile.write(buffer);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        cachefile.write(buffer, offset, length);
    }

    /**
     * 将表单文件数据零拷贝到指定文件
     */
    public long transfer(File dst) throws IOException {
        return cachefile.transfer(dst);
    }

    /**
     * 将表单文件数据重命名
     */
    public long rename(File dst) throws IOException {
        return cachefile.rename(dst);
    }

    public void delete() throws IOException {
        cachefile.close();
    }
}
