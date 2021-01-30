package cloud.apposs.webx;

import cloud.apposs.util.Encoder;
import cloud.apposs.util.Parser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * 防XSS攻击过滤器
 * 启动此XSS防御会在HttpServletRequest请求去到Handler前进行XSS字符转义
 */
public class XssFilter implements Filter {
    /** Web.XML配置的是否开启XSS防御，默认为关闭 */
    public static final String ENABLE = "enable";
    private static boolean isEnable = false;

    @Override
    public void init(FilterConfig config) throws ServletException {
        isEnable = Parser.parseBoolean(config.getInitParameter(ENABLE), false);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(new XssRequest((HttpServletRequest) request), response);
    }

    @Override
    public void destroy() {
    }

    public static class XssRequest extends HttpServletRequestWrapper {
        public XssRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String name) {
            if (!isEnable) {
                return super.getParameterValues(name);
            }
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            int count = values.length;
            String[] encodedValues = new String[count];
            for (int i = 0; i < count; i++) {
                encodedValues[i] = Encoder.encodeJavaScript(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String name) {
            if (!isEnable) {
                return super.getParameter(name);
            }
            String value = super.getParameter(name);
            if (value != null) {
                value = Encoder.encodeJavaScript(value);
            }
            return value;
        }

        @Override
        public Object getAttribute(String name) {
            if (!isEnable) {
                return super.getAttribute(name);
            }
            Object value = super.getAttribute(name);
            if (value != null && value instanceof String) {
                Encoder.encodeJavaScript((String) value);
            }
            return value;
        }

        @Override
        public String getHeader(String name) {
            if (!isEnable) {
                return super.getHeader(name);
            }
            String value = super.getHeader(name);
            if (value != null) {
                value = Encoder.encodeJavaScript(value);
            }
            return value;
        }
    }
}
