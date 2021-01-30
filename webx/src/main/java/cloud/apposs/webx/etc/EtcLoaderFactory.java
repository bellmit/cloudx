package cloud.apposs.webx.etc;

public interface EtcLoaderFactory {
	int getLoaderType();
	
	EtcLoader getLoader(int cacheTime, Object... args) throws EtcLoadException;
}
