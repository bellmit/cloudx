package cloud.apposs.webx.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.MediaType;
import cloud.apposs.webx.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IoBuffer字节码输出视图渲染器，
 * 让业务层直接以IoBuffer的方式返回，底层则将读取IoBuffer字节码并响应输出
 */
@Component
public class IoBufferViewResolver extends AbstractViewResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supports(HttpServletRequest request, HttpServletResponse response, Object result) {
        return (result instanceof IoBuffer);
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response, Object result, boolean flush) throws Exception {
        WebUtil.response(request, response, MediaType.APPLICATION_OCTET_STREAM, config.getCharset(), ((IoBuffer) result), flush);
    }
}
