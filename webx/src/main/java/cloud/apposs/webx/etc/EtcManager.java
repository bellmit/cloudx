package cloud.apposs.webx.etc;

import cloud.apposs.util.Param;
import cloud.apposs.util.SysUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Json配置管理器，负责管理Web业务中所有的配置文件，
 * 实现根据配置更改之后的实时读取或者定时更新读取
 * 通过文件配置或者QConf配置中心配置实现，
 * 为了避免加载配置异常影响业务，如果加载配置出错则返回null并调用业务方成自定义监听器处理错误信息
 */
public final class EtcManager {
	public static final int ETC_TYPE_FILE = 0;
	public static final int ETC_TYPE_ZOOKEEPER = 1;
	public static final int ETC_TYPE_QCONF = 2;
	
	/**
	 * 配置内容缓存时间，在该时间段内的内容不会判断内容是否修改，直接返回旧内容，
	 * 默认为1分钟
	 */
	private static final int DEFAULT_CACHE_TIME = 60 * 1000;
	
	/**
	 * {@link EtcLoader}工厂，业务方可实现自己的工厂模式并注入到配置管理器中
	 */
	private final Map<Integer, EtcLoaderFactory> loaderFactorys = 
		new ConcurrentHashMap<Integer, EtcLoaderFactory>();
	
	/**
     * 加载器监听列表
     */
    private final List<EtcLoaderListener> loaderListeners = 
    	new LinkedList<EtcLoaderListener>();
	
	/**
	 * 一个配置Key对应一个加载器
	 */
	private final Map<String, EtcLoader> loaderPool =
		new ConcurrentHashMap<String, EtcLoader>();
	
	public EtcManager() {
		loaderFactorys.put(ETC_TYPE_FILE, new FileEtcLoader.FileEtcLoaderFactory());
	}
	
	public void addLoaderFactory(EtcLoaderFactory loaderFactory) {
		SysUtil.checkNotNull(loaderFactory, "loaderFactory");
		loaderFactorys.put(loaderFactory.getLoaderType(), loaderFactory);
	}
	
	public void addLoaderListener(EtcLoaderListener loaderListener) {
		SysUtil.checkNotNull(loaderListener, "loaderListener");
		loaderListeners.add(loaderListener);
	}
	
	public final boolean addConfig(String key) {
		return addConfig(key, DEFAULT_CACHE_TIME, ETC_TYPE_FILE, key);
	}
	
	public final boolean addConfig(String key, String filePath) {
		return addConfig(key, ETC_TYPE_FILE, DEFAULT_CACHE_TIME, filePath);
	}
	
	/**
	 * 设置文件配置
	 * 
	 * @param  key  配置KEY
	 * @param  type 以什么形式设置配置，有文件配置和QConf配置
	 * @param  args 配置参数，文件配置时为文件路径
	 * @return 设置成功返回true
	 */
	public final boolean addConfig(String key, int type, int cacheTime, Object... args) {
		EtcLoader loader = null;
		try {
			EtcLoaderFactory loaderFactory = loaderFactorys.get(type);
			if (loaderFactory == null) {
				throw new EtcLoadException("Conf Loader[" + type + "] Not Found Error;");
			}
			loader = loaderFactory.getLoader(cacheTime, args);
			loader.loadEtc();
			loaderPool.put(key, loader);
			if (loader == null) {
				return false;
			}
			fireEtcLoaderInit(loader);
			return true;
		} catch(Throwable t) {
			fireEtcLoaderError(loader, t);
			return false;
		}
	}
	
	public final boolean addConfig(String key, EtcLoader loader) {
		SysUtil.checkNotNull(loader, "loader");
		try {
			loader.loadEtc();
			loaderPool.put(key, loader);
			fireEtcLoaderInit(loader);
			return true;
		} catch(Throwable t) {
			fireEtcLoaderError(loader, t);
			return false;
		}
	}
	
	/**
	 * 从配置器中获取配置信息，
	 * 如果配置内容修改之后格式异常则返回旧的配置内容，避免当配置内容格式出错时影响业务
	 */
	public final Param getConfig(String key) {
		Param oldConfig = null;
		EtcLoader loader = null;
		try {
			loader = loaderPool.get(key);
			if (loader == null) {
				throw new EtcLoadException("Conf Key[" + key + "] Not Found Error;");
			}
		
			oldConfig = loader.getEtc();
			// 从配置器中获取配置内容
			if (!loader.isEtcModified()) {
				return oldConfig;
			}
			loader.loadEtc();
			fireEtcLoaderRefresh(loader);
			Param newConfig = loader.getEtc();
			return newConfig;
		} catch(EtcLoadException e) {
			fireEtcLoaderError(loader, e);
			return oldConfig;
		}
	}
	
	private void fireEtcLoaderInit(EtcLoader loader) {
		for (EtcLoaderListener loaderListenrer : loaderListeners) {
			loaderListenrer.loadInit(loader);
		}
	}
	
	private void fireEtcLoaderRefresh(EtcLoader loader) {
		for (EtcLoaderListener loaderListenrer : loaderListeners) {
			loaderListenrer.loadRefresh(loader);
		}
	}
	
	private void fireEtcLoaderError(EtcLoader loader, Throwable t) {
		for (EtcLoaderListener loaderListenrer : loaderListeners) {
			loaderListenrer.loadError(loader, t);
		}
	}
}
