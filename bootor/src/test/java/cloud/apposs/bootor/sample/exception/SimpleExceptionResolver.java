package cloud.apposs.bootor.sample.exception;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.WebExceptionResolver;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 简单异常拦截，仅打印异常堆栈到终端
 */
@Component
public class SimpleExceptionResolver implements WebExceptionResolver<HttpRequest, HttpResponse> {
    @Override
    public Object resolveHandlerException(HttpRequest request, HttpResponse response, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
