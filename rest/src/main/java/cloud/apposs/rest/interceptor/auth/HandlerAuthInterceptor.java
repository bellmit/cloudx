package cloud.apposs.rest.interceptor.auth;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.auth.AuthDefine;
import cloud.apposs.rest.auth.AuthManager;
import cloud.apposs.rest.auth.AuthPermission;
import cloud.apposs.rest.auth.AuthPermissionNotSetException;
import cloud.apposs.rest.auth.AuthUser;
import cloud.apposs.rest.auth.AuthUserNotFoundException;
import cloud.apposs.rest.interceptor.HandlerInterceptorAdapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户权限验证拦截器
 * 使用方法：
 * 1、实现{@link cloud.apposs.rest.auth.Authentication}会话权限并用{@link cloud.apposs.ioc.annotation.Component}注解注入到框架IOC容器中
 * 2、{@link cloud.apposs.rest.RestConfig#isAuthDriven()}配置开启权限拦截注解
 * 3、Action类在要判断权限的地方添加{@link Auth}权限注解，注意权限拦截之前用户必须要已经登录
 */
public class HandlerAuthInterceptor<R, P> extends HandlerInterceptorAdapter<R, P> {
    /**
     * Action配置权限认证缓存
     */
    private final Map<Handler, AuthPermission> handlerPermissions = new ConcurrentHashMap<Handler, AuthPermission>();

    @Override
    public boolean preHandle(R request, P response, Handler handler) throws Exception {
        Auth authAnnotation = handler.getAnnotation(Auth.class);
        // Action没有配置Auth认证注解，直接跳过权限验证
        if (authAnnotation == null) {
            return true;
        }
        AuthPermission permission = null;
        // 先从缓存获取权限，如果没有再查找对应配置的权限
        if (handlerPermissions.containsKey(handler)) {
            permission = handlerPermissions.get(handler);
        } else {
            String authValue = authAnnotation.value();
            String[] auths = authValue.split(AuthPermission.AUTH_PERMISION_MERGE);
            permission = AuthDefine.createEmptyAuthPermission();
            // 如果业务方配置Auth("CS"|"UI")等注解，则需要拆分并合并成AuthPermission
            for (int i = 0; i < auths.length; i++) {
                String auth = auths[i].trim();
                AuthPermission thePermission = AuthManager.getAuthPermission(auth);
                // 指定的用户权限不存在
                if (thePermission == null) {
                    throw new AuthPermissionNotSetException();
                }
                permission.addAuthPermission(thePermission);
            }
            handlerPermissions.put(handler, permission);
        }
        // 获取登录用户信息
        AuthUser authUser = AuthManager.getAuthUser(request, response);
        if (authUser == null) {
            throw new AuthUserNotFoundException();
        }
        // 进行权限验证
        return authUser.isPermitted(permission);
    }
}
