package cloud.apposs.rest.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证用户，业务方可继承扩展该类
 */
public class AuthUser {
	protected final String sessionId;
	
	/** 登录用户ID */
	protected final int aid;
	
	/** 登录员工ID */
	protected final int sid;
	
	/** 
	 * 用户标志位，
	 * 一般用于设置用户信息，例如是否禁止登录，是否为内部账号等
	 */
	protected final long flag;
	
	/** 用户权限 */
	protected final AuthPermission permission;
	
	/** 登录IP，有可能会变 */
	protected int ip;
	
	/** 登录时间 */
	protected final long loginTime;
	
	/** 最近操作时间 */
	protected long actionTime;
	
	/** 当前会话请求存储的一些状态值 */
	private final Map<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();
	
	public AuthUser(String sessionId, int aid, int sid, 
			long flag, AuthPermission permission) {
		this.sessionId = sessionId;
		this.aid = aid;
		this.sid = sid;
		this.flag = flag;
		this.permission = permission;
		this.loginTime = System.currentTimeMillis();
		this.actionTime = System.currentTimeMillis();
	}
	
	public String getSessionId() {
		return sessionId;
	}

	public int getAid() {
		return aid;
	}

	public int getSid() {
		return sid;
	}

	public int getIp() {
		return ip;
	}

	public void setIp(int ip) {
		this.ip = ip;
	}
	
	public long getLoginTime() {
		return loginTime;
	}

	public long getActionTime() {
		return actionTime;
	}

	public void setActionTime(long actionTime) {
		this.actionTime = actionTime;
	}
	
	public boolean isFlagEnable(long flag) {
		return (this.flag & flag) == flag;
	}
	
	public boolean isFlagDisable(long flag) {
		return (this.flag & flag) != flag;
	}
	
	public final Object getAttribute(Object key) {
        return getAttribute(key, null);
    }
	
	public final Object getAttribute(Object key, Object defaultVal) {
        Object attr = attributes.get(key);
        if (attr == null && defaultVal != null) {
        	attr = defaultVal;
        	attributes.put(key, attr);
        }
        return attr;
    }
	
	public final Object setAttribute(Object key, Object value) {
        return attributes.put(key, value);
    }
	
	public final boolean hasAttribute(Object key) {
        return attributes.containsKey(key);
    }
	
	/**
	 * 添加指定权限
	 */
	public boolean addAuthPermission(AuthPermission permission) {
		return this.permission.addAuthPermission(permission);
	}
	
	/**
	 * 移除指定权限
	 */
	public boolean removeAuthPermission(AuthPermission permission) {
		return this.permission.removeAuthPermission(permission);
	}

	/**
	 * 检查对应的权限是否通过
	 */
	public boolean isPermitted(AuthPermission permission) {
		if (permission == null) {
			return false;
		}
		long group = this.permission.group();
		long role = this.permission.role();
		long permitGroup = permission.group();
		long permitRole = permission.role();
		return (group & permitGroup) == permitGroup && (role & permitRole) == permitRole;
	}
}
