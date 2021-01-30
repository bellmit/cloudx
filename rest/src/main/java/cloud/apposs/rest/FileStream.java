package cloud.apposs.rest;

import cloud.apposs.util.CachedFileStream;
import cloud.apposs.util.MediaType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件传输类封装，
 * 服务于{@link cloud.apposs.rest.view.ViewResolver}中的视图解析，其服务功能如下
 * 1、主要应用于文件下载业务，提供数据零拷贝传输提升JVM性能
 * 2、下载模式，即当前类存储为File即表示直接文件本地读取并供客户端下载
 * 3、代理模式，即当前为存储为CachedFileStream即表示从其他地方网络请求数据并读取保存供客户端下载
 */
public final class FileStream {
    /**
     * 多媒体传输文件类型
     */
    private final MediaType mediaType;

    /**
     * 多媒体传输文件，此时为普通文件下载模式
     */
    private final File mediaFile;

    /**
     * 多媒体传输文件，此时为先下载再供其他客户端下载模式
     */
    private final CachedFileStream downloadFile;

    /**
     * 多媒体传输自定义响应Header
     */
    private final Map<String, String> headers = new HashMap<String, String>();

    public static FileStream create(String mediaFile) {
        return new FileStream(new File(mediaFile));
    }

    public static FileStream create(File mediaFile) {
        return new FileStream(mediaFile);
    }

    public static FileStream create(MediaType mediaType, String mediaFile) {
        return new FileStream(mediaType, new File(mediaFile));
    }

    public static FileStream create(MediaType mediaType, File mediaFile) {
        return new FileStream(mediaType, mediaFile);
    }

    public static FileStream create(CachedFileStream downloadFile) {
        return new FileStream(downloadFile);
    }

    public static FileStream create(MediaType mediaType, CachedFileStream downloadFile) {
        return new FileStream(mediaType, downloadFile);
    }

    public FileStream(File mediaFile) {
        this(MediaType.APPLICATION_OCTET_STREAM, mediaFile);
    }

    public FileStream(MediaType mediaType, File mediaFile) {
        this.mediaType = mediaType;
        this.mediaFile = mediaFile;
        this.downloadFile = null;
    }

    public FileStream(CachedFileStream downloadFile) {
        this(MediaType.APPLICATION_OCTET_STREAM, downloadFile);
    }

    public FileStream(MediaType mediaType, CachedFileStream downloadFile) {
        this.mediaType = mediaType;
        this.mediaFile = null;
        this.downloadFile = downloadFile;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * 文件是否为从其他地方网络请求数据并读取保存供客户端下载
     */
    public boolean isMediaMode() {
        return mediaFile != null;
    }

    public File getMediaFile() {
        return mediaFile;
    }

    /**
     * 文件是否为从其他地方网络请求数据并读取保存供客户端下载
     */
    public boolean isDownloadMode() {
        return downloadFile != null;
    }

    public CachedFileStream getDownloadFile() {
        return downloadFile;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public FileStream putHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }
}
