package cloud.apposs.okhttp;

import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.util.CharsetUtil;

import java.nio.charset.Charset;

/**
 * HTTP请求编辑器
 */
public final class HttpBuilder {
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    public static final int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
    public static final int DEFAULT_RETRY_COUNT = 3;
    public static final int DEFAULT_RETRY_SLEEP_TIME = 200;

    /**
     * 服务发现组件
     */
    private IDiscovery discovery;

    /**
     * 异步IO轮询池数量
     */
    private int loopSize = Runtime.getRuntime().availableProcessors();;

    /**
     * 请求连接超时时间，默认5S
     */
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /**
     * 数据网络读写超时时间，默认60S
     */
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;

    /**
     * HTTP请求失败后的重试次数，为0则不重试
     */
    private int retryCount = DEFAULT_RETRY_COUNT;

    /**
     * HTTP请求失败后重试的休眠失败，避免雪崩
     */
    private int retrySleepTime = DEFAULT_RETRY_SLEEP_TIME;

    /**
     * HTTP请求/响应编码
     */
    private Charset charset = CharsetUtil.UTF_8;

    public static HttpBuilder builder() {
        return new HttpBuilder();
    }

    public IDiscovery discovery() {
        return discovery;
    }

    public HttpBuilder discovery(IDiscovery discovery) {
        this.discovery = discovery;
        return this;
    }

    /**
     * 建立异步HTTP请求服务
     */
    public OkHttp build() throws Exception {
        return new OkHttp(this);
    }

    public int loopSize() {
        return loopSize;
    }

    public HttpBuilder loopSize(int loopSize) {
        this.loopSize = loopSize;
        return this;
    }

    public int connectTimeout() {
        return this.connectTimeout;
    }

    public HttpBuilder connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int socketTimeout() {
        return this.socketTimeout;
    }

    public HttpBuilder socketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public int retryCount() {
        return retryCount;
    }

    public HttpBuilder retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public int retrySleepTime() {
        return retrySleepTime;
    }

    public HttpBuilder retrySleepTime(int retrySleepTime) {
        this.retrySleepTime = retrySleepTime;
        return this;
    }

    public Charset charset() {
        return charset;
    }

    public HttpBuilder charset(Charset charset) {
        this.charset = charset;
        return this;
    }
}
