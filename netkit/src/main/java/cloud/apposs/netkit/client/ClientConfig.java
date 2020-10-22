package cloud.apposs.netkit.client;

import cloud.apposs.configure.ConfigurationFactory;
import cloud.apposs.configure.ConfigurationParser;
import cloud.apposs.logger.Appender;
import cloud.apposs.logger.Configuration;
import cloud.apposs.logger.Logger;

import java.io.File;
import java.util.Properties;

public class ClientConfig {
	private String filename;
	
	/** 业务名称，便于通过日志区分不同业务请求 */
	private String name;
	
	/** 远程主机地址 */
	private String host = "0.0.0.0";
	/** 远程主机端口 */
	private int port = -1;
	
	private int connectTimeout = 2 * 1000;
	private int recvTimeout = 60 * 1000;
	private int sendTimeout = 60 * 1000;
	
	/**
	 * 开启此参数，那么客户端在每次发送数据时，无论数据包的大小都会将这些数据发送出 去
	 * 参考：
	 * http://blog.csdn.net/huang_xw/article/details/7340241
	 * http://www.open-open.com/lib/view/open1412994697952.html
	 */
	private boolean tcpNoDelay = true;
	
	/** 日志输出终端 */
	private String logAppender = Appender.CONSOLE;
	
	/** 
	 * 日志输出级别，
	 * FATAL（致命）、
	 * ERROR（错误）、
	 * WARN（警告）、
	 * INFO（信息）、
	 * DEBUG（调试）、
	 * OFF（关闭），
	 * 默认为INFO
	 */
	private String logLevel = "INFO";
	
	/** 日志的存储路径 */
	private String logPath = "log";
	
	/** 日志输出模板 */
	private String logFormat = "%d{yyyy-MM-dd HH:mm:ss} %F:%M:%L[%p] %m%n%e";
	
	public ClientConfig() {
	}
	
	public ClientConfig(String filename) throws Exception {
		this.filename = filename;
		if (filename.toLowerCase().endsWith(".xml")) {
			ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.XML);
			cp.parse(this, filename);
		} else {
			ConfigurationParser cp = ConfigurationFactory.getConfigurationParser(ConfigurationFactory.JSON);
			cp.parse(this, filename);
		}
		initialize();
	}
	
	public static ClientConfig parseConfig(String filename) throws Exception {
		ClientConfig config = new ClientConfig(filename);
		return config;
	}
	
	public String getFilename() {
		return filename;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getRecvTimeout() {
		return recvTimeout;
	}

	public void setRecvTimeout(int recvTimeout) {
		this.recvTimeout = recvTimeout;
	}

	public int getSendTimeout() {
		return sendTimeout;
	}

	public void setSendTimeout(int sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public boolean isTcpNoDelay() {
		return tcpNoDelay;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public String getLogAppender() {
		return logAppender;
	}

	public void setLogAppender(String logAppender) {
		this.logAppender = logAppender;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getLogFormat() {
		return logFormat;
	}

	public void setLogFormat(String logFormat) {
		this.logFormat = logFormat;
	}
	
	public void initialize() {
		// 初始化日志
		Properties logProp = new Properties();
		logProp.put(Configuration.Prefix.APPENDER, logAppender);
		logProp.put(Configuration.Prefix.LEVEL, logLevel);
		if (!logPath.endsWith(File.separator)) {
			logPath += File.separator;
		}
		logPath += "{yyyy-MM-dd}.log";
		logProp.put(Configuration.Prefix.FILE, logPath);
		logProp.put(Configuration.Prefix.FORMAT, logFormat);
		Logger.config(logProp);
	}
}