package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

/**
 * 纯文本参数
 */
public class LiteralVariable implements IVariable {
    private final String literal;

    public LiteralVariable(String literal) {
        this.literal = literal;
    }

    @Override
    public String parse(HttpRequest request, HttpResponse response) {
        return literal;
    }
}
