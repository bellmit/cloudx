package cloud.apposs.bootor.resolver.view;

import cloud.apposs.bootor.WebUtil;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.MediaType;

/**
 * IoBuffer字节码输出视图渲染器，
 * 让业务层直接以IoBuffer的方式返回，便于采用数据零拷贝进行数据传输，
 * 对JVM内存没有压力，提升服务性能，
 * HTTP协议实现中HEADER返回contetype-type为stream，BODY为文件二进制流
 */
@Component
public class IoBufferViewResolver extends AbstractViewResolver<HttpRequest, HttpResponse> {
    @Override
    public boolean supports(HttpRequest request, HttpResponse response, Object result) {
        return (result instanceof IoBuffer);
    }

    @Override
    public void render(HttpRequest request, HttpResponse response, Object result, boolean flush) throws Exception {
        WebUtil.response(response, MediaType.APPLICATION_OCTET_STREAM, config.getCharset(), ((IoBuffer) result), flush);
    }
}
