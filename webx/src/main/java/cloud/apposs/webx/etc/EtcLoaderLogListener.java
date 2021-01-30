package cloud.apposs.webx.etc;

import cloud.apposs.logger.Logger;

public class EtcLoaderLogListener extends EtcLoaderListenerAdaptor {
	@Override
	public void loadInit(EtcLoader loader) {
		Logger.info("EtcLoadr Initial;Loader=%s", loader);
	}

	@Override
	public void loadRefresh(EtcLoader loader) {
		Logger.info("EtcLoadr Refresh;Loader=%s", loader);
	}

	@Override
	public void loadError(EtcLoader loader, Throwable t) {
		Logger.error(t, "EtcLoader Load Error[%s];;Loader=%s", loader);
	}
}
