package cloud.apposs.okhttp;

import java.util.LinkedList;
import java.util.List;

public class HttpInterceptorSupport {
    /**
     * 创建一个拦截器列表（用于存放拦截器实例）
     */
    private List<IHttpInterceptor> interceptorList = new LinkedList<IHttpInterceptor>();

    public void addInterceptor(IHttpInterceptor interceptor) {
        interceptorList.add(interceptor);
    }

    public void removeInterceptor(IHttpInterceptor interceptor) {
        interceptorList.remove(interceptor);
    }

    public void preRequest(IORequest request) throws Exception {
        for (IHttpInterceptor interceptor : interceptorList) {
            interceptor.preRequest(request);
        }
    }
}
