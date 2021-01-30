package cloud.apposs.gateway.handler.index;

import cloud.apposs.gateway.handler.AbstractHandler;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.Param;

/**
 * 直接内容返回服务，对应return指令
 */
public class ReturnHandler extends AbstractHandler {
    /**
     * HTTP响应状态
     */
    private int status = HttpStatus.HTTP_STATUS_200.getCode();

    /**
     * 输出HTML内容
     */
    private String content;

    @Override
    public void initialize(Param options) throws Exception {
        this.status = options.getInt("status");
        this.content = options.getString("content");
    }

    @Override
    public RxIo<String> handle(HttpRequest request, HttpResponse response) throws Exception {
        response.getHeaders().putAll(addHeaders);
        response.setStatus(HttpStatus.getStatus(status));
        return RxIo.from(content);
    }
}
