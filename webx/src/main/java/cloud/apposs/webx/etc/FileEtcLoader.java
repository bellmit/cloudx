package cloud.apposs.webx.etc;

import cloud.apposs.util.FileUtil;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

import java.io.File;

/**
 * 文件配置加载器，线程安全
 */
public class FileEtcLoader extends AbstractEtcLoader {
	private String filePath;
	
	/** 文件修改时间 ，主要用于判断文件配置是否更新 */
	private long lastModified = 0;
	
	public FileEtcLoader(String filePath, int cacheTime) throws EtcLoadException {
		super(cacheTime);
		this.filePath = filePath;
	}
	
	@Override
	public boolean doCheckEtcModified() {
		File file = new File(filePath);
		long newLastModified = file.lastModified();
		return newLastModified != lastModified;
	}

	@Override
	public boolean doLoadEtc() throws EtcLoadException {
		// 缓存过期了并且文件发生了更新
		synchronized (this) {
			File file = new File(filePath);
			if (!file.isFile() || !file.exists()) {
				throw new EtcLoadException("Conf File[" + filePath + "] Not Found Error;");
			}
			// 双重检测，避免重入
			long fileLastModified = file.lastModified();
			if (fileLastModified == lastModified) {
				return false;
			}
			
			String json = FileUtil.readString(file);
			if (json == null) {
				throw new EtcLoadException("Conf File[" + filePath + "] Read Error;");
			}
			Param newConfig = JsonUtil.parseJsonParam(json);
			if (newConfig == null) {
				throw new EtcLoadException("Conf File[" + filePath + "] Parse Error;");
			}
			
			updateTime = System.currentTimeMillis();
			lastModified = fileLastModified;
			config = newConfig;
			return true;
		}
	}
	
	public static final class FileEtcLoaderFactory implements EtcLoaderFactory {
		@Override
		public EtcLoader getLoader(int cacheTime, Object... args)
				throws EtcLoadException {
			if (args.length <= 0 || !(args[0] instanceof String) || StrUtil.isEmpty(((String) args[0]))) {
				throw new EtcLoadException("Conf File Args Error;");
			}
			String filePath = (String) args[0];
			return new FileEtcLoader(filePath, cacheTime);
		}

		@Override
		public int getLoaderType() {
			return EtcManager.ETC_TYPE_FILE;
		}
	}
}
