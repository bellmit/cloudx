package cloud.apposs.bootor.listener.httplog.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;
import cloud.apposs.util.StrUtil;

/**
 * 请求头部，对应参数：$http_xxx_xxx
 */
public class HttpHeaderVariable extends AbstractVariable {
    private final String header;

    public HttpHeaderVariable(String header) {
        this.header = header;
    }

    @Override
    public String parse(HttpRequest request, HttpResponse response, Handler handler, Throwable t) {
        String value = request.getHeader(header);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value;
    }
}
