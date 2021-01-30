package cloud.apposs.webx.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.CachedFileStream;
import cloud.apposs.util.MediaType;
import cloud.apposs.webx.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * IoBuffer字节码输出视图渲染器，
 * 让业务层直接以IoBuffer的方式返回，底层则将读取IoBuffer字节码并响应输出
 */
@Component
public class FileViewResolver extends AbstractViewResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supports(HttpServletRequest request, HttpServletResponse response, Object result) {
        return (result instanceof FileStream);
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response, Object result, boolean flush) throws Exception {
        FileStream fileStream = (FileStream) result;
        Map<String, String> headers = fileStream.getHeaders();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
        MediaType mediaType = fileStream.getMediaType();
        String charset = config.getCharset();
        if (fileStream.isMediaMode()) {
            // 此时业务逻辑为纯下载文件而已，即下载模式
            WebUtil.response(request, response, mediaType, charset, fileStream.getMediaFile(), flush);
        } else {
            // 此时业务逻辑为先下载文件再提供文件下载给客户端下载，即代理模式
            CachedFileStream downloadFile = fileStream.getDownloadFile();
            if (downloadFile.isInMemory()) {
                WebUtil.response(request, response, mediaType, charset, downloadFile.getRawData(), flush);
            } else {
                WebUtil.response(request, response, mediaType, charset, downloadFile.getRawFile(), flush);
            }
        }
    }
}
