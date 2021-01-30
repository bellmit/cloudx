package cloud.apposs.webx.resource;

import cloud.apposs.util.Param;

/**
 * 自定义资源管理实现，包括
 * 如何生成资源域名、资源加载等
 */
public interface Resource {
	public static final int RES_TYPE_JS = 0;
	public static final int RES_TYPE_CSS = 1;
	
	/**
	 * 根据不同的类型获取不同的资源域名/二级域名，例如
	 * JS/CSS/IMAGE/MP4等不同的资源域名，
	 * 之所以不同资源用不同域名在于给资源文件增加多个域名，可以提高浏览器的并发请求能力
	 * <pre>
	 * 示例：
	 * 系统官网资源域名:ss.faisys.com
	 * 用户图片资源域名:[aid].s21i.faiusr.com
	 * 用户下载资源域名:[aid].s21d.faiusrd.com
	 * </pre>
	 * 
	 * @param type 资源类型
	 */
	String getResDomain(int type);
	
	/**
	 * 根据资源Key决定返回什么资源类型，例如
	 * js_jquery -> type[js]
	 * css_bootstrap -> type[css]
	 */
	int getResType(String key);
	
	/**
	 * 加载系统资源文件配置，在系统启动时自动调用
	 */
	void loadResList();
	
	/**
	 * 获取所有的资源配置列表
	 */
	Param getResList();
}
