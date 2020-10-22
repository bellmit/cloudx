package cloud.apposs.configure;

import java.io.InputStream;

public interface ConfigurationParser {
	/**
	 * 通过配置文件解析配置反射映射到对象模型中
	 */
	void parse(Object model, String filename) throws Exception;
	void parse(Object model, InputStream filestream) throws Exception;
}
