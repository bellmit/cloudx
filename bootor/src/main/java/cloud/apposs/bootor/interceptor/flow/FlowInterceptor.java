package cloud.apposs.bootor.interceptor.flow;

import cloud.apposs.bootor.BootorConstants;
import cloud.apposs.bootor.interceptor.BooterInterceptorAdaptor;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StandardResult;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class FlowInterceptor extends BooterInterceptorAdaptor {
    /**
     * 每个请求的流水号生成器
     */
    private final AtomicLong flow = new AtomicLong(0);

    @Override
    public boolean preHandle(HttpRequest request, HttpResponse response, Handler handler) throws Exception {
        // 生成流水号
        // 判断网站是否有传递_flow流水号，
        // 设计此在于如果业务方需要调试时可自己自定义流水号来调试问题
        Object flowValue = request.getParameter(BootorConstants.REQUEST_PARAMETER_FLOW);
        if (flowValue == null) {
            flowValue = request.getParam().get(BootorConstants.REQUEST_PARAMETER_FLOW);
        }
        long flow = flowValue != null ? Parser.parseLong(flowValue.toString(), -1) : -1;
        if (flow < 0) {
            if (this.flow.incrementAndGet() >= Long.MAX_VALUE) {
                this.flow.set(1);
            }
            flow = this.flow.get();
        }
        request.setAttribute(BootorConstants.REQUEST_PARAMETRIC_FLOW, flow);
        return true;
    }

    @Override
    public void afterCompletion(HttpRequest request, HttpResponse response,
                                Handler handler, Object result, Throwable throwable) {
        if (result instanceof StandardResult) {
            request.setAttribute(BootorConstants.REQUEST_ATTRIBUTE_ERRNO, ((StandardResult) result).getErrno().value());
        }
    }
}
