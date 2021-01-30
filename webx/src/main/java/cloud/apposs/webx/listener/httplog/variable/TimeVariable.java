package cloud.apposs.webx.listener.httplog.variable;

import cloud.apposs.rest.Handler;
import cloud.apposs.webx.WebXConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求远程主机，对应参数：$host
 */
public class TimeVariable extends AbstractVariable {
    @Override
    public String parse(HttpServletRequest request, HttpServletResponse response, Handler handler, Throwable t) {
        Object attrValue = request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_START_TIME);
        // 在异步线程请求里面，有可能当前逻辑处理进来之前，线程池可能因为请求超时或者异常先释放了请求，需要做空判断保护
        if (!(attrValue instanceof Long)) {
            return "0";
        }
        long startTime = (long) attrValue;
        return String.valueOf(System.currentTimeMillis() - startTime);
    }
}
