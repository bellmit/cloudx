package cloud.apposs.gateway.interceptor.auth;

import cloud.apposs.gateway.WebUtil;
import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.gateway.interceptor.HandlerInterceptorAdapter;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.Encryptor;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.Param;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

/**
 * 请求鉴权拦截器，业务方必须提供APP-ID、TOKEN和FLOW的HEADER头才允许请求到网关后端
 */
public class AuthTokenInterceptor extends HandlerInterceptorAdapter {
    private String salt = "RkBpc2Mw";

    @Override
    public void init(Param arguments) {
        if (arguments.containsKey("salt")) {
            this.salt = arguments.getString("salt");
        }
    }

    @Override
    public boolean preHandle(HttpRequest request, HttpResponse response, IHandler handler) throws Exception {
        String appId = request.getHeader("AppId");
        String token = request.getHeader("Token");
        long flow = Parser.parseLong(request.getHeader("Flow"), -1);
        if (StrUtil.isEmpty(appId) || StrUtil.isEmpty(token) || flow <= 0) {
            Logger.warn("Need auth token request with URI [%s]", WebUtil.getRequestPath(request));
            response.setStatus(HttpStatus.HTTP_STATUS_403);
            response.flush();
            return false;
        }

        if (!token.equals(Encryptor.md5(appId + salt))) {
            Logger.warn("Invalid auth token %s with URI [%s]", token, WebUtil.getRequestPath(request));
            response.setStatus(HttpStatus.HTTP_STATUS_403);
            response.flush();
            return false;
        }
        return true;
    }
}
