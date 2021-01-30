package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.bootor.BootorConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;

/**
 * 异常解析，对应参数：$exp
 */
public class ExceptionVariable extends AbstractVariable {
    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        if (t == null) {
            return null;
        }
        return t.toString();
    }
}
