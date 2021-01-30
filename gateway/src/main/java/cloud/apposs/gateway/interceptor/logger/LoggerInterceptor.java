package cloud.apposs.gateway.interceptor.logger;

import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.gateway.interceptor.HandlerInterceptorAdapter;
import cloud.apposs.gateway.variable.VariableParser;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.Param;
import cloud.apposs.util.Parser;

public class LoggerInterceptor extends HandlerInterceptorAdapter {
    /**
     * 是否开启请求日志拦截
     */
    private boolean enable = true;

    /**
     * 日期解析器
     */
    private VariableParser parser;

    @Override
    public void init(Param arguments) {
        if (arguments.containsKey("enable")) {
            enable = Parser.parseBoolean(arguments.getString("enable"));
        }
        String format = "$remote_addr:$remote_port $host";
        if (arguments.containsKey("format")) {
            format = arguments.getString("format");
        }
        parser = new VariableParser(format);
    }

    @Override
    public void afterCompletion(HttpRequest request, HttpResponse response, IHandler handler, Throwable throwable) {
        if (enable) {
            if (throwable != null) {
                Logger.error(throwable, parser.parse(request, response));
            } else {
                Logger.info(parser.parse(request, response));
            }
        }
    }
}
