package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.StrUtil;

/**
 * 请求头部，对应参数：$http_xxx_xxx
 */
public class HttpHeaderVariable implements IVariable {
    private final String header;

    public HttpHeaderVariable(String header) {
        this.header = header;
    }

    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        String value = request.getHeader(header);
        if (StrUtil.isEmpty(value)) {
            return "-";
        }
        return value;
    }
}
