package cloud.apposs.webx.sample.action;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.auth.AuthDefine;
import cloud.apposs.rest.auth.AuthPermission;
import cloud.apposs.rest.auth.AuthUser;
import cloud.apposs.rest.auth.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserAuth implements Authentication<HttpServletRequest, HttpServletResponse> {
    @Override
    public Map<String, AuthPermission> getAuthPermissionList() {
        Map<String, AuthPermission> permissionList = new HashMap<>();
        permissionList.put("ADMIN", AuthDefine.AUTH_SUPER_ADMIN);
        return permissionList;
    }

    @Override
    public String getSessionId(HttpServletRequest request) {
        return "xcSYoKDnNzcVa2s0";
    }

    @Override
    public AuthUser getAuthUser(String sessionId) {
        return new AuthUser("xcSYoKDnNzcVa2s0", 1, 1, ~1, AuthDefine.AUTH_SUPER_ADMIN);
    }
}
