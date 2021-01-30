package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.util.StrUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求内部属性获取，主要为内部系统设置，对应参数：$attr_xxx_xxx
 */
public class HttpAttributeVariable extends AbstractVariable {
    private final String attribute;

    public HttpAttributeVariable(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        Object value = request.getAttribute(attribute);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value.toString();
    }
}
