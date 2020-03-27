package cloud.apposs.logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件资源工具类
 */
public class ResourceUtil {
	/**
	 * 获取一个文件输入流
	 */
	public static InputStream getInputStream(final String path) throws IOException {
		return getInputStream(path, ResourceUtil.class);
	}
	
	public static InputStream getInputStream(final String path, final Class<?> clazz) throws IOException {
		InputStream is = null;
		
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		if (contextClassLoader!=null) {
			is = contextClassLoader.getResourceAsStream(path);
		}
		if (is != null) {
			return is;
		}
		
		is = clazz.getClassLoader().getResourceAsStream(path);
		if (is != null) {
			return is;
		}
		
		is = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
		if (is != null) {
			return is;
		}
		
		return new File(path).toURI().toURL().openStream();
	}
}
