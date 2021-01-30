package cloud.apposs.gateway;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;

public final class WebUtil {
	/**
     * 获取请求路径
     */
    public static String getRequestPath(HttpRequest request) {
        String servletPath = request.getRequestUri();
        return servletPath;
    }
}
