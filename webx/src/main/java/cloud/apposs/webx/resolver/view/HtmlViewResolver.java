package cloud.apposs.webx.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.util.MediaType;
import cloud.apposs.webx.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 页面Url跳转/转发视图渲染器
 */
@Component
@Order(Integer.MAX_VALUE)
public class HtmlViewResolver extends AbstractViewResolver<HttpServletRequest, HttpServletResponse> {
    /**
     * 如果没有其他视图匹配，默认该视图为处理所有业务逻辑返回结果的处理视图
     */
    @Override
    public boolean supports(HttpServletRequest request, HttpServletResponse response, Object result) {
        return true;
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response, Object result, boolean flush) throws Exception {
        WebUtil.response(request, response, MediaType.TEXT_HTML, config.getCharset(), result.toString(), flush);
    }
}
