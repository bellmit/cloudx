package cloud.apposs.webx.listener.httplog;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.listener.httplog.HandlerLogListener;
import cloud.apposs.webx.WebXConstants;
import cloud.apposs.webx.listener.httplog.variable.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求日志监听输出，支持自定义HTTP请求日志格式输出
 */
@Component
public class HttpLogHandlerListener extends HandlerLogListener<HttpServletRequest, HttpServletResponse> {
    @Override
    public void initialize(RestConfig config) {
        super.initialize(config);
        String logFormat = config.getHttpLogFormat();
        this.parser = new VariableParser(logFormat);
    }

    @Override
    public void setStartTime(HttpServletRequest request, HttpServletResponse response, Handler handler) {
        request.setAttribute(WebXConstants.REQUEST_ATTRIBUTE_START_TIME, System.currentTimeMillis());
    }
}
