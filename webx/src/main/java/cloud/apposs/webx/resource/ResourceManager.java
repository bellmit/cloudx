package cloud.apposs.webx.resource;

import cloud.apposs.util.Param;

/**
 * 资源文件管理器
 */
public final class ResourceManager {
	private static final String ERROR_RES_KEY = "error_";
	
	private static Resource resource;
	
	public static void setResource(Resource resource) {
		ResourceManager.resource = resource;
	}
	
	public static Resource getResource() {
		return resource;
	}
	
	/**
	 * 获取资源文件路径，如果资源路径没配置则返回/error_[key]
	 */
	public static String getResPath(String key) {
		if (resource == null) {
			return ERROR_RES_KEY + key;
		}
		int type = resource.getResType(key);
		return getResPath(type, key);
	}
	
	/**
	 * 获取资源文件路径，如果资源路径没配置则返回/error_[key]
	 * 
	 * @param type 资源类型，CSS/JS等
	 * @param key  资源配置Key
	 */
	public static String getResPath(int type, String key) {
		if (resource == null) {
			return ERROR_RES_KEY + key;
		}
		String resDomain = resource.getResDomain(type);
		if (resDomain == null) {
			return ERROR_RES_KEY + key;
		}
		Param resList = resource.getResList();
		String resPath = resList.getString(key);
		if (resPath == null) {
			return ERROR_RES_KEY + key;
		}
		return resDomain + resPath;
	}
	
	/**
	 * 获取资源域名
	 */
	public static String getResDomain(int type) {
		if (resource == null) {
			return "";
		}
		String resDomain = resource.getResDomain(type);
		return resDomain == null ? "" : resDomain;
	}
}
