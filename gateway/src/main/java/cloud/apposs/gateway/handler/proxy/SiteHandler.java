package cloud.apposs.gateway.handler.proxy;

import cloud.apposs.gateway.handler.AbstractHandler;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.okhttp.HttpBuilder;
import cloud.apposs.okhttp.IORequest;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.util.Param;

/**
 * 直接反向代理到指定网站，对应site_pass指令
 */
public class SiteHandler extends AbstractHandler {
    private String proxyUrl;

    private OkHttp okHttp;

    @Override
    public void initialize(Param options) throws Exception {
        proxyUrl = options.getString("proxyUrl");
        okHttp = HttpBuilder.builder().build();
    }

    @Override
    public RxIo<?> handle(HttpRequest request, HttpResponse response) throws Exception {
        IORequest clientRequest = IORequest.builder().url(proxyUrl).addHeaders(proxyHeaders);
        return okHttp.execute(clientRequest).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer httpAnswer) throws Exception {
                response.getHeaders().putAll(addHeaders);
                return httpAnswer.getContent();
            }
        });
    }

    @Override
    public void close() {
        okHttp.close();
    }
}
