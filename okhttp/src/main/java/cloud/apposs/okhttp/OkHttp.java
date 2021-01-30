package cloud.apposs.okhttp;

import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.OnSubscribeIo;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.io.http.IoHttp;
import cloud.apposs.netkit.rxio.io.http.IoHttpMultipy;
import cloud.apposs.registry.ServiceInstance;
import cloud.apposs.util.StrUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.zip.GZIPInputStream;

/**
 * 客户端异步HTTP请求，
 * 主要对IoHttp的封装，增加了如下功能：
 * <pre>
 * 1、服务发现和故障转移，底层用Discovery组件
 * 2、IoHttp接口的统一封装，供业务开发使用
 * 3、实现代理转发服务
 * </pre>
 * 注意：组件内部维护了EventLoop异步轮询器，每个业务模块HTTP请求只对应一个OkHttp实例，即单例
 */
@Component
public final class OkHttp {
    public static final String DEFAULT_LOOP_NAME = "Http_Event_Loop-";

    /**
     * HTTP异步轮询池
     */
    private final EventLoopGroup loop;

    /**
     * 主要服务于是HTTP异步请求重试时的异步休眠
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 服务发现模块
     */
    private final HttpBuilder builder;

    private final HttpInterceptorSupport interceptorSupport = new HttpInterceptorSupport();

    public OkHttp(HttpBuilder builder) throws Exception {
        int loopSize = builder.loopSize();
        this.loop = new EventLoopGroup(loopSize, DEFAULT_LOOP_NAME).start(true);
        if (builder.retryCount() > 0) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "OkHttp-Scheduler");
                }
            });
        } else {
            this.scheduler = null;
        }
        IDiscovery discovery = builder.discovery();
        if (discovery != null) {
            discovery.start();
        }
        this.builder = builder;
    }

    /**
     * HTTP GET异步请求，底层采用EventLoop
     */
    public RxIo<HttpAnswer> execute(String url) throws Exception {
        return execute(IORequest.builder().url(url));
    }

    /**
     * HTTP异步请求，底层采用EventLoop
     */
    public RxIo<HttpAnswer> execute(IORequest request) throws Exception {
        IoHttp ioHttp = doCreateIoHttp(request);
        // 如果没有配置重试则不用创建异步重试接口，否则创建
        if (builder.retryCount() <= 0) {
            return RxIo.create(new OnSubscribeIo<HttpAnswer>(loop, ioHttp));
        } else {
            return RxIo.create(new OnSubscribeIo<HttpAnswer>(loop, ioHttp)).retry(new HttpRetry(request));
        }
    }

    /**
     * HTTP同步GET请求，底层采用HttpURLConnection，
     * 一般不建议网络采用同步调用方法，除非业务确实特殊
     */
    public String perform(String url) throws Exception {
        return perform(IORequest.builder().url(url));
    }

    /**
     * HTTP同步请求，底层采用HttpURLConnection，
     * 一般不建议网络采用同步调用方法，除非业务确实特殊
     */
    public String perform(IORequest request) throws Exception {
        String url = request.url();
        URI uri = URI.create(url);

        // 设置代理
        String serviceId = request.serviceId();
        Object key = request.key();
        IDiscovery discovery = builder.discovery();
        Proxy proxy = null;
        if (discovery != null && !StrUtil.isEmpty(serviceId)) {
            ServiceInstance instance = discovery.choose(serviceId, key);
            if (instance == null) {
                throw new IOException("no avaiable service '" + serviceId + "' of uri '"+ uri + "'");
            }
            proxy = doGetProxy(request, instance);
        }

        // 创建连接
        URL httpUrl = new URL(url);
        HttpURLConnection connection = null;
        try {
            if (proxy == null) {
                connection = (HttpURLConnection) httpUrl.openConnection();
            } else {
                connection = (HttpURLConnection) httpUrl.openConnection(proxy);
            }
            // 设置为http短连接，否则java http底层会在短时间内复用连接导致代理异常
            connection.setRequestProperty("Connection", "close");
            // 设置header参数
            Map<String, String> headers = request.getHeaders();
            for (String header : headers.keySet()) {
                connection.setRequestProperty(header, headers.get(header));
            }
            connection.setRequestMethod(request.method().toString());
            // 设置超时时间
            connection.setConnectTimeout(builder.connectTimeout());
            connection.setReadTimeout(builder.socketTimeout());
            // 表单POST数据编码
            FormEntity formEntity = request.formEntity();
            if (formEntity != null) {
                List<IoBuffer> bufferList = new LinkedList<IoBuffer>();
                formEntity.getForm().addRequestBuffer(bufferList);
                OutputStream os = connection.getOutputStream();
                for (IoBuffer buffer : bufferList) {
                    os.write(buffer.array());
                }
                os.flush();
                os.close();
            }
            InputStream httpInStream = new BufferedInputStream(connection.getInputStream());
            // 响应数据是否需要GZIP解压
            String contentEncode = connection.getContentEncoding();
            if (!StrUtil.isEmpty(contentEncode) && contentEncode.contains("gzip")) {
                httpInStream = new GZIPInputStream(httpInStream);
            }
            // 读取HTTP响应数据
            StringBuilder content = new StringBuilder(1024 * 6);
            Reader httpReader = new InputStreamReader(httpInStream, builder.charset());
            int c;
            while ((c = httpReader.read()) != -1) {
                content.append((char) c);
            }
            return content.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void addInterceptor(IHttpInterceptor interceptor) {
        interceptorSupport.addInterceptor(interceptor);
    }

    /**
     * 组装HTTP异步请求
     */
    private IoHttp doCreateIoHttp(IORequest request) throws Exception {
        String url = request.url();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        String serviceId = request.serviceId();
        Object key = request.key();
        IDiscovery discovery = builder.discovery();
        // 如果有配置服务发现则从服务发现中获取对应的可用负载均衡节点
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (host == null) {
            host = uri.getAuthority();
        }
        if (host == null) {
            throw new IllegalArgumentException(uri + " hostname is null");
        }
        // 设置代理
        Proxy proxy = null;
        if (discovery != null && !StrUtil.isEmpty(serviceId)) {
            ServiceInstance instance = discovery.choose(serviceId, key);
            if (instance == null) {
                throw new IOException("no avaiable service '" + serviceId + "' of uri '"+ uri + "'");
            }
            proxy = doGetProxy(request, instance);
            if (proxy == null) {
                // 采用代理的直接请求过去即可，如果不是用代理则代表使用的是服务发现，需要替换成真实的服务发现实例
                String instanceHost = instance.getHost();
                int instancePort = instance.getPort();
                url = url.replace(host, instanceHost + ":" + instancePort);
            }
        }

        // 开始建立HTTP连接
        interceptorSupport.preRequest(request);
        IoHttp ioHttp = new IoHttpMultipy(url, request.method(), proxy);
        ioHttp.addHeader("Host", host);
        ioHttp.addHeaders(request.getHeaders());
        ioHttp.setConnectTimeout(builder.connectTimeout());
        ioHttp.setSendTimeout(builder.socketTimeout());
        ioHttp.setRecvTimeout(builder.socketTimeout());
        if (request.formEntity() != null) {
            ioHttp.setForm(request.formEntity().getForm());
        }

        return ioHttp;
    }

    /**
     * 判断是否走代理
     */
    private Proxy doGetProxy(IORequest request, ServiceInstance service) throws IOException {
        String serviceHost = service.getHost();
        int servicePort = service.getPort();
        int proxyMode = request.proxyMode();
        // 判断是否是改成走代理
        if (proxyMode == IORequest.ProxyMode.SOCKS) {
            InetSocketAddress proxyAddr = new InetSocketAddress(serviceHost, servicePort);
            return new Proxy(Proxy.Type.SOCKS, proxyAddr);
        } else if (proxyMode == IORequest.ProxyMode.HTTP) {
            InetSocketAddress proxyAddr = new InetSocketAddress(serviceHost, servicePort);
            return new Proxy(Proxy.Type.HTTP, proxyAddr);
        }
        return null;
    }

    public void close() {
        loop.shutdown();
        IDiscovery discovery = builder.discovery();
        if (discovery != null) {
            discovery.shutdown();
        }
    }

    private class HttpRetry implements IoFunction<Throwable, RxIo<HttpAnswer>> {
        private final IORequest request;

        private int current = 0;

        public HttpRetry(IORequest request) {
            this.request = request;
        }

        @Override
        public RxIo<HttpAnswer> call(Throwable throwable) throws Exception {
            IoHttp ioHttp = doCreateIoHttp(request);
            // 超过最大重试次数，不再重试
            if (++current > builder.retryCount()) {
                return null;
            }
            // 重新创建HTTP请求并异步休眠一段时间再执行避免后端负载压力过大
            int sleepTime = (int) (Math.random() * (current * builder.retrySleepTime()));
            Logger.warn("remote address '%s' transmission fail, retry %d in %d milliseconds",
                    request.url(), current, sleepTime);
            return RxIo.create(new OnSubscribeIo<HttpAnswer>(loop, ioHttp)).sleep(scheduler, sleepTime);
        }
    }
}
