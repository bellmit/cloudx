package cloud.apposs.webx;

import cloud.apposs.rest.WebExceptionResolver;
import cloud.apposs.util.StrUtil;
import cloud.apposs.webx.resolver.exception.SimpleExceptionResolver;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web请求异常过滤器，负责对Web所有异常进行处理
 */
public class WebExceptionFilter implements Filter {
    /**
     * Web.XML配置的异常拦截实现类
     */
    private static final String INIT_PARAMETER_EXCEPTION_HANDLER_CLASS = "exceptionHandlerClass";

    private WebExceptionResolver<HttpServletRequest, HttpServletResponse> exceptionResolver = new SimpleExceptionResolver();

    @Override
    @SuppressWarnings("unchecked")
    public void init(FilterConfig config) throws ServletException {
        String handlerClass = config.getInitParameter(INIT_PARAMETER_EXCEPTION_HANDLER_CLASS);
        if (!StrUtil.isEmpty(handlerClass)) {
            try {
                Class<?> exceptionResolverClass = Class.forName(handlerClass);
                exceptionResolver = (WebExceptionResolver) exceptionResolverClass.newInstance();
            } catch (Exception e) {
                throw new ServletException("Initialization WebExceptionFilter Error", e);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain chain) throws ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) rsp;
        try {
            chain.doFilter(request, response);
        } catch (Throwable throwable) {
            exceptionResolver.resolveHandlerException(request, response, throwable);
        }
    }

    @Override
    public void destroy() {
    }
}
