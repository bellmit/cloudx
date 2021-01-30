package cloud.apposs.rest.listener.httplog.variable;

import cloud.apposs.rest.Handler;

public class LiteralVariable<R, P> implements IVariable<R, P> {
    private final String literal;

    public LiteralVariable(String literal) {
        this.literal = literal;
    }

    @Override
    public String parse(R request, P response, Handler handler, Throwable t) {
        return literal;
    }
}
