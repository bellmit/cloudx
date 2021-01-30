package cloud.apposs.bootor.resolver.view;

import cloud.apposs.bootor.WebUtil;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.CachedFileStream;
import cloud.apposs.util.MediaType;

import java.util.Map;

/**
 * 文件字节码输出视图渲染器，
 * 让业务层直接以IoBuffer的方式返回，便于采用数据零拷贝进行数据传输，
 * 对JVM内存没有压力，提升服务性能，
 * HTTP协议实现中HEADER返回contetype-type为stream，BODY为文件二进制流
 */
@Component
public class FileViewResolver extends AbstractViewResolver<HttpRequest, HttpResponse> {
    @Override
    public boolean supports(HttpRequest request, HttpResponse response, Object result) {
        return (result instanceof FileStream);
    }

    @Override
    public void render(HttpRequest request, HttpResponse response, Object result, boolean flush) throws Exception {
        FileStream fileStream = (FileStream) result;
        Map<String, String> headers = fileStream.getHeaders();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.putHeader(entry.getKey(), entry.getValue());
        }
        MediaType mediaType = fileStream.getMediaType();
        String charset = config.getCharset();
        if (fileStream.isMediaMode()) {
            // 此时业务逻辑为纯下载文件而已，即下载模式
            WebUtil.response(response, mediaType, charset, fileStream.getMediaFile(), flush);
        } else {
            // 此时业务逻辑为先下载文件再提供文件下载给客户端下载，即代理模式
            CachedFileStream downloadFile = fileStream.getDownloadFile();
            if (downloadFile.isInMemory()) {
                WebUtil.response(response, mediaType, charset, downloadFile.getRawData(), flush);
            } else {
                WebUtil.response(response, mediaType, charset, downloadFile.getRawFile(), flush);
            }
        }
    }
}
