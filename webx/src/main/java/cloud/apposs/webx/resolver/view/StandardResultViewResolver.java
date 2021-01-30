package cloud.apposs.webx.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;
import cloud.apposs.util.StandardResult;
import cloud.apposs.webx.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Json格式输出视图渲染器
 */
@Component
public class StandardResultViewResolver extends AbstractViewResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supports(HttpServletRequest request, HttpServletResponse response, Object result) {
        return (result instanceof StandardResult);
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response, Object result, boolean flush) throws Exception {
        WebUtil.response(request, response, MediaType.APPLICATION_JSON, config.getCharset(), ((StandardResult)result).toJson(), flush);
    }
}
