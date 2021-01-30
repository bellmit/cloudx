package cloud.apposs.rest.auth;

import cloud.apposs.util.StrUtil;

import java.util.Map;

/**
 * 用户会话权限管理，会话认证核心类，
 * 实现包括注册、登录、认证、授权、加密等会话管理
 */
@SuppressWarnings("unchecked")
public class AuthManager {
    protected static Authentication authentication;

    protected static Map<String, AuthPermission> authPermissionList;

    /**
     * 获取业务方认证实现类
     */
    public static Authentication getAuthentication() {
        return authentication;
    }

    /**
     * 设置业务方认证实现类
     */
    public static void setAuthentication(Authentication authentication) {
        AuthManager.authentication = authentication;
    }

    /**
     * 初始化相关认证属性
     */
    public static void initialize() throws AuthenticationException {
        if (authentication != null) {
            authPermissionList = authentication.getAuthPermissionList();
            if (authPermissionList == null) {
                throw new AuthPermissionNotSetException();
            }
        }
    }

    /**
     * 获取登录的用户信息
     */
    public static <R, P> AuthUser getAuthUser(R request, P response) throws AuthenticationException {
        if (authentication == null) {
            throw new AuthenticationNoSetException();
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            throw new AuthUserNotLoginException();
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        if (authUser == null) {
            throw new AuthUserNotFoundException();
        }
        return authUser;
    }

    /**
     * 判断用户是否登录，
     * 不抛出任何异常
     */
    public static <R, P> boolean isLoginSession(R request, P response) {
        if (authentication == null) {
            return false;
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return false;
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        return authUser != null;
    }

    /**
     * 检查用户会话权限
     */
    public static <R, P> boolean checkAuthPermission(
            R request, P response, AuthPermission permission) throws AuthenticationException {
        if (authentication == null) {
            throw new AuthenticationNoSetException();
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            throw new AuthUserNotLoginException();
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        if (authUser == null) {
            throw new AuthUserNotFoundException();
        }
        // 开始检查用户权限
        return authUser.isPermitted(permission);
    }

    public static <R, P> boolean isAuthPermission(R request, P response, String auth) {
        if (authentication == null) {
            return false;
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            return false;
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        if (authUser == null) {
            return false;
        }
        // 开始检查用户权限
        AuthPermission permission = AuthManager.getAuthPermission(auth);
        if (permission == null) {
            return false;
        }
        return authUser.isPermitted(permission);
    }

    /**
     * 添加用户会话权限
     */
    public static <R, P> boolean addAuthPermission(
            R request, P response, AuthPermission permission) throws AuthenticationException {
        if (authentication == null) {
            throw new AuthenticationNoSetException();
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            throw new AuthUserNotLoginException();
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        if (authUser == null) {
            throw new AuthUserNotFoundException();
        }
        // 开始添加用户权限
        return authUser.addAuthPermission(permission);
    }

    /**
     * 移除用户会话权限
     */
    public static <R, P> boolean removeAuthPermission(
            R request, P response, AuthPermission permission) throws AuthenticationException {
        if (authentication == null) {
            throw new AuthenticationNoSetException();
        }
        // 通过传递的会话ID获取用户信息
        String sessionId = authentication.getSessionId(request);
        if (StrUtil.isEmpty(sessionId)) {
            throw new AuthUserNotLoginException();
        }
        AuthUser authUser = authentication.getAuthUser(sessionId);
        if (authUser == null) {
            throw new AuthUserNotFoundException();
        }
        // 开始移除用户权限
        return authUser.removeAuthPermission(permission);
    }

    /**
     * 获取所有的权限列表
     */
    public static Map<String, AuthPermission> getAuthPermissionList() {
        return authPermissionList;
    }

    /**
     * 获取指定配置的权限
     */
    public static AuthPermission getAuthPermission(String auth) {
        return authPermissionList.get(auth);
    }
}
