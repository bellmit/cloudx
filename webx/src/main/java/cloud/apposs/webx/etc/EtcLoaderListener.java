package cloud.apposs.webx.etc;

import java.util.EventListener;

public interface EtcLoaderListener extends EventListener {
	/**
	 * 配置初步加载的监听
	 */
	void loadInit(EtcLoader loader);

	/**
	 * 配置加载异常的监听，注意Loader参数可能为空
	 */
	void loadError(EtcLoader loader, Throwable t);
	
	/**
	 * 配置重新加载的监听
	 */
	void loadRefresh(EtcLoader loader);
}
