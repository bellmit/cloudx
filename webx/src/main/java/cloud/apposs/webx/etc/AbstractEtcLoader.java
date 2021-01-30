package cloud.apposs.webx.etc;

import cloud.apposs.util.Param;

public abstract class AbstractEtcLoader implements EtcLoader {
	/** 缓存时间多久，-1为永久缓存 */
	protected int cacheTime;
	
	/** 缓存最近更新时间 */
	protected long updateTime;
	
	/** 配置内容 */
	protected Param config;
	
	public AbstractEtcLoader(int cacheTime) {
		this.cacheTime = cacheTime;
		this.updateTime = System.currentTimeMillis();
	}
	
	@Override
	public Param getEtc() {
		return config;
	}
	
	@Override
	public boolean isEtcModified() {
		// 文件没超过缓存时间
		if (cacheTime < 0 || (System.currentTimeMillis() - updateTime) < cacheTime) {
			return false;
		}
		return doCheckEtcModified();
	}

	@Override
	public boolean loadEtc() throws EtcLoadException {
		return doLoadEtc();
	}
	
	public abstract boolean doCheckEtcModified();
	
	public abstract boolean doLoadEtc() throws EtcLoadException;
}
