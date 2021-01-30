package cloud.apposs.webx.etc;

import cloud.apposs.util.Param;

/**
 * 配置加载器，
 * 可以实现从
 * 1、Json配置文件加载
 * 2、Property配置文件加载
 * 3、DB配置文件加载
 * 4、QConf配置加载
 */
public interface EtcLoader {
	/**
	 * 直接获取配置
	 */
	Param getEtc();
	
	/**
	 * 判断文件是否已经被更改，如果被更改则调用{{@link #loadEtc()}进行配置重载
	 */
	boolean isEtcModified();
	
	/**
	 * 加载配置，如果配置内容有更新，可以更新加载
	 */
	boolean loadEtc() throws EtcLoadException;
}
