package cloud.apposs.webx.resolver.view;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.view.AbstractViewResolver;
import cloud.apposs.rest.view.ViewResolver;
import cloud.apposs.webx.WebUtil;
import cloud.apposs.webx.WebXConfig;
import cloud.apposs.webx.WebXConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 页面Url跳转/转发视图渲染器
 */
@Component
public class UrlViewResolver extends AbstractViewResolver<HttpServletRequest, HttpServletResponse> {
    public static final String REQUEST_ATTRIBUTE_URL_CONTENT = "_AttrUrlContent";
    public static final String REQUEST_ATTRIBUTE_URL_DIRECT = "_AttrUrlDirect";

    /** 制定JSP页面存放的路径 **/
    private String urlPrefix;

    /** 制定JSP文件页面的后缀 **/
    private String urlSuffix;

    @Override
    public ViewResolver build(RestConfig config) {
        super.build(config);
        WebXConfig webXConfig = (WebXConfig) config.getAttachment();
        urlPrefix = webXConfig.getUrlPrefix();
        urlSuffix = webXConfig.getUrlSuffix();
        return this;
    }

    @Override
    public boolean supports(HttpServletRequest request, HttpServletResponse response, Object result) {
        if (result instanceof String) {
            String content = (String) result;
            if (content.startsWith(WebXConstants.REDIRECT_URL_PREFIX)) {
                // 页面重定向视图
                String urlRequest = content.substring(WebXConstants.REDIRECT_URL_PREFIX.length());
                request.setAttribute(REQUEST_ATTRIBUTE_URL_CONTENT, urlRequest);
                request.setAttribute(REQUEST_ATTRIBUTE_URL_DIRECT, true);
                return true;
            } else if (content.startsWith(WebXConstants.FORWARD_URL_PREFIX)) {
                // 页面转发视图
                String urlRequest = content.substring(WebXConstants.FORWARD_URL_PREFIX.length());
                request.setAttribute(REQUEST_ATTRIBUTE_URL_CONTENT, urlRequest);
                request.setAttribute(REQUEST_ATTRIBUTE_URL_DIRECT, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(HttpServletRequest request, HttpServletResponse response, Object result, boolean flush) throws Exception {
        String urlRequest = (String) request.getAttribute(REQUEST_ATTRIBUTE_URL_CONTENT);
        boolean urlRedirect = (boolean) request.getAttribute(REQUEST_ATTRIBUTE_URL_DIRECT);
        if (urlRedirect) {
            String url = createTargetUrl(urlRequest, request);
            WebUtil.sendRedirect301(request, response, url);
        } else {
            String url = urlPrefix + urlRequest + urlSuffix;
            WebUtil.sendForward(request, response, url);
        }
    }

    private String createTargetUrl(String url, HttpServletRequest request) {
        StringBuilder targetUrl = new StringBuilder();
        if (url.startsWith("/")) {
            targetUrl.append(request.getContextPath());
        }
        targetUrl.append(url);
        return targetUrl.toString();
    }
}
