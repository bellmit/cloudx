package cloud.apposs.rest.auth;

import cloud.apposs.util.StrUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 认证权限，业务方可继承扩展该类
 * 认证用户和认证权限是多对多关系，
 * 即一个用户可对应多个权限，一个权限可对应多个用户
 */
public class AuthPermission {
    public static final String AUTH_PERMISION_MERGE = "\\|";

    /**
     * 认证组别权限标志，采用取位标志来识别
     */
    private long group;

    /**
     * 认证角色权限标志位，采用取位标志来识别，每个角色权限必须要先属于组权限
     */
    private long role;

    /**
     * 权限描述
     */
    protected final List<String> descriptions = new LinkedList<String>();

    public AuthPermission(long group, long role) {
        this(group, role, null);
    }

    public AuthPermission(long group, long role, String description) {
        this.group = group;
        this.role = role;
        if (!StrUtil.isEmpty(description)) {
            this.descriptions.add(description);
        }
    }

    public long group() {
        return group;
    }

    public long role() {
        return role;
    }

    public List<String> descriptions() {
        return descriptions;
    }

    /**
     * 添加指定权限
     */
    public boolean addAuthPermission(AuthPermission permission) {
        if (permission == null) {
            return false;
        }
        long newGroup = group | permission.group();
        long newRole = role | permission.role();
        if (newGroup == group && newRole == role) {
            return false;
        }
        descriptions.addAll(permission.descriptions());
        return true;
    }

    /**
     * 移除指定权限
     */
    public boolean removeAuthPermission(AuthPermission permission) {
        if (permission == null) {
            return false;
        }
        long newGroup = group & ~permission.group();
        long newRole = role & ~permission.role();
        if (newGroup == group && newRole == role) {
            return false;
        }
        descriptions.removeAll(permission.descriptions());
        return true;
    }

    @Override
    public String toString() {
        return descriptions.toString();
    }
}
