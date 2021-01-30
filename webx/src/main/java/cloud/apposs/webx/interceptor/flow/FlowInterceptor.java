package cloud.apposs.webx.interceptor.flow;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.Handler;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StandardResult;
import cloud.apposs.webx.WebXConstants;
import cloud.apposs.webx.interceptor.WebXInterceptorAdaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FlowInterceptor extends WebXInterceptorAdaptor {
    /**
     * 每个请求的流水号生成器
     */
    private final AtomicLong flow = new AtomicLong(0);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Handler handler) throws Exception {
        // 生成流水号
        // 判断网站是否有传递_flow流水号，
        // 设计此在于如果业务方需要调试时可自己自定义流水号来调试问题
        long flow = Parser.parseLong(request.getParameter(WebXConstants.REQUEST_PARAMETER_FLOW), -1);
        if (flow < 0) {
            if (this.flow.incrementAndGet() >= Long.MAX_VALUE) {
                this.flow.set(1);
            }
            flow = this.flow.get();
        }
        request.setAttribute(WebXConstants.REQUEST_PARAMETRIC_FLOW, flow);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Handler handler, Object result, Throwable throwable) {
        if (result instanceof StandardResult) {
            request.setAttribute(WebXConstants.REQUEST_ATTRIBUTE_ERRNO, ((StandardResult) result).getErrno().value());
        }
    }
}
