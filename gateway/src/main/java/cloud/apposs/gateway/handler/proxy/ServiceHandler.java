package cloud.apposs.gateway.handler.proxy;

import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.discovery.QconfDiscovery;
import cloud.apposs.discovery.ZooKeeperDiscovery;
import cloud.apposs.gateway.handler.AbstractHandler;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.io.http.HttpMethod;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.HttpBuilder;
import cloud.apposs.okhttp.IORequest;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.util.Param;

import java.util.Map;

/**
 * 基于服务发现的反向代理，对应service_pass指令
 */
public class ServiceHandler extends AbstractHandler {
    private String serviceId;

    private OkHttp okHttp;

    @Override
    public void initialize(Param options) throws Exception {
        serviceId = options.getString("serviceId");
        String registry = options.getString("registry");
        String environment = options.getString("environment");
        String path = options.getString("path");
        IDiscovery discovery = null;
        if (registry.equalsIgnoreCase("zookeeper")) {
            discovery = new ZooKeeperDiscovery(environment, path);
        } else {
            discovery = new QconfDiscovery(environment, path);
        }
        okHttp = HttpBuilder.builder().loopSize(1).discovery(discovery).build();
    }

    @Override
    public RxIo<String> handle(HttpRequest request, HttpResponse response) throws Exception {
        FormEntity formEntity = FormEntity.builder();
        Map<String, String> parameters = request.getParameters();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            formEntity.add(entry.getKey(), entry.getValue());
        }
        IORequest clientRequest = IORequest.builder()
                .url(request.getRequestUrl())
                .addHeaders(proxyHeaders)
                .request(HttpMethod.getHttpMethod(request.getMethod()), formEntity)
                .serviceId(serviceId).key(request.getRemoteAddr().toString());
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
