package cloud.apposs.rest.listener.httplog.variable;

import cloud.apposs.rest.Handler;

public interface IVariable<R, P> {
    /**
     * 解析对应的配置参数，示例：$remote_addr/$remote_port/$http_user_agent等
     */
    String parse(R request, P response, Handler handler, Throwable t);
}
