package cloud.apposs.bootor.listener.httplog;

import cloud.apposs.bootor.BootorConstants;
import cloud.apposs.bootor.listener.httplog.variable.VariableParser;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.RestConfig;
import cloud.apposs.rest.listener.httplog.HandlerLogListener;

/**
 * 请求日志监听输出，支持自定义HTTP请求日志格式输出
 */
@Component
public class HttpLogHandlerListener extends HandlerLogListener<HttpRequest, HttpResponse> {
    @Override
    public void initialize(RestConfig config) {
        super.initialize(config);
        String logFormat = config.getHttpLogFormat();
        this.parser = new VariableParser(logFormat);
    }

    @Override
    public void setStartTime(HttpRequest request, HttpResponse response, Handler handler) {
        request.setAttribute(BootorConstants.REQUEST_ATTRIBUTE_START_TIME, System.currentTimeMillis());
    }
}
