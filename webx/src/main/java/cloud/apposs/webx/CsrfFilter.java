package cloud.apposs.webx;

import cloud.apposs.util.Encryptor;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 防CSRF攻击过滤器模块，各业务接口可以根据业务实际情况基于此模板修改
 * 启动此CWRF防御会在HttpServletRequest请求去到Handler前进行TOKEN验证
 * 参考：
 * https://www.ibm.com/developerworks/cn/web/1102_niugang_csrf/
 * https://blog.csdn.net/qq_39338799/article/details/85274706
 */
public class CsrfFilter implements Filter {
    /** Web.XML配置的是否开启XSS防御，默认为关闭 */
    public static final String ENABLE = "enable";
    /** 前端请求过来的TOKEN参数 */
    public static final String PARAMETER_TOKEN = "_TOKEN";
    /** TOKEN值为md5(sessionId+TOKEN_TAIL),可以认为是一个不变的加密seed，而session id每次登录都变 */
    public final static String TOKEN_TAIL = "@T0KEN_";

    private static boolean isEnable = false;
    private static String[] whiteUriList = new String[]{"login", "register"};

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        isEnable = Parser.parseBoolean(filterConfig.getInitParameter(ENABLE), false);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {
        if (isEnable) {
            HttpServletRequest request = (HttpServletRequest) req;
            ServletResponse response = resp;
            String uri = request.getRequestURI();
            // 过滤不检查TOKEN的请求
            boolean checkToken = false;
            for (int i = 0; i < whiteUriList.length; i++) {
                checkToken = uri.indexOf(whiteUriList[i]) < 0;
                if (checkToken) {
                    break;
                }
            }
            if (checkToken) {
                // 获取用户登录后的SESSION_ID
                String sessionId = "_SESSIONID_";
                String token = request.getParameter(PARAMETER_TOKEN);
                boolean isCsrfAttack = StrUtil.isEmpty(token) || !token.equals(Encryptor.md5(sessionId + TOKEN_TAIL));
                if (isCsrfAttack) {
                    // 传递过来的TOKEN为空，可能是CSRF攻击，输出页面错误
                    response.getOutputStream().print("{\"success\":false,\"msg\":\"request forbidden\"}");
                    return;
                }
            }
        }
        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
    }
}
