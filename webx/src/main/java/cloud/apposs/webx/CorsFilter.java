package cloud.apposs.webx;

import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 跨域问题解决，参考：
 * 跨域资源共享 CORS 详解: http://www.ruanyifeng.com/blog/2016/04/cors.html
 * 图解CORS: https://mp.weixin.qq.com/s/6BPYgnTOw-cOysMp-otfbQ
 */
public class CorsFilter implements Filter {
    /** Web.XML配置的是否开启XSS防御，默认为关闭 */
    public static final String PARAM_ENABLE = "enable";
    /** 配置允许请求过来的跨域域名，默认为*即全部允许 */
    public static final String PARAM_ALLOW_ORIGIN = "allowOrigin";
    /** 配置允许的请求METHOD，可配置POST, GET, PUT */
    public static final String PARAM_ALLOW_METHODS = "allowMethods";
    /** 配置允许的请求HEADER，可配置Origin, X-Requested-With, X-Auth-Token */
    public static final String PARAM_ALLOW_HEADERS = "allowHeaders";

    private boolean isEnable = false;
    private String allowOrigin = "*";
    private String allowMethods = "POST, GET, PUT, OPTIONS, DELETE";
    private String allowHeaders = "Origin, X-Requested-With, X-Auth-Token, Content-Type, Accept";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.isEnable = Parser.parseBoolean(filterConfig.getInitParameter(PARAM_ENABLE), false);
        if (!StrUtil.isEmpty(filterConfig.getInitParameter(PARAM_ALLOW_ORIGIN))) {
            this.allowOrigin = filterConfig.getInitParameter(PARAM_ALLOW_ORIGIN);
        }
        if (!StrUtil.isEmpty(filterConfig.getInitParameter(PARAM_ALLOW_METHODS))) {
            this.allowMethods = filterConfig.getInitParameter(PARAM_ALLOW_METHODS);
        }
        if (!StrUtil.isEmpty(filterConfig.getInitParameter(PARAM_ALLOW_HEADERS))) {
            this.allowHeaders = filterConfig.getInitParameter(PARAM_ALLOW_HEADERS);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws ServletException, IOException {
        if (isEnable) {
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            HttpServletRequest request = (HttpServletRequest) servletRequest;

            String requestHost = request.getRemoteHost();
            String requestOrigin = request.getHeader("Origin");
            if (allowOrigin.equals("*") || allowOrigin.contains(requestHost)) {
                response.setContentType("application/json; charset=utf-8");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Access-Control-Max-Age", "3600");
                response.setHeader("Access-Control-Allow-Methods", allowMethods);
                response.setHeader("Access-Control-Allow-Origin", requestOrigin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
                response.setHeader("Access-Control-Allow-Headers", allowHeaders);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }
}
