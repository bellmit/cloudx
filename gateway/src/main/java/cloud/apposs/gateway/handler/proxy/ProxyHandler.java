package cloud.apposs.gateway.handler.proxy;

import cloud.apposs.balance.Peer;
import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.discovery.MemoryDiscovery;
import cloud.apposs.gateway.GatewayConfig;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 基于配置代理集群的反向代理转发服务，对应proxy_pass指令
 */
public class ProxyHandler extends AbstractHandler {
    private String serviceId;

    private OkHttp okHttp;

    @Override
    public void initialize(Param options) throws Exception {
        serviceId = options.getString("key");
        List<GatewayConfig.UpstreamServer> upstreamServerList = options.getList("upstream");
        Map<String, List<Peer>> peers = new HashMap<String, List<Peer>>();
        List<Peer> peerList = new LinkedList<Peer>();
        for (GatewayConfig.UpstreamServer upstreamServer : upstreamServerList) {
            peerList.add(new Peer(upstreamServer.getHost(), upstreamServer.getPort()));
        }
        peers.put(serviceId, peerList);
        IDiscovery discovery = new MemoryDiscovery(peers);
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
