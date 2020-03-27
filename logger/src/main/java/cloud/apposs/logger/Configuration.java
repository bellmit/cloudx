package cloud.apposs.logger;

import cloud.apposs.logger.appender.ConsoleAppender;
import cloud.apposs.logger.appender.DatabaseAppender;
import cloud.apposs.logger.appender.FileAppender;

import java.io.InputStream;
import java.util.Properties;

/**
 * 日志全局配置
 */
public final class Configuration {
	public static final class Prefix {
		public static final String LEVEL = "log.level";
		public static final String NAME = "log.name";
		public static final String APPENDER = "log.appender";
		
		public static final String FORMAT = "log.format";
		public static final String FILE = "log.file";
		
		public static final String DRIVER = "log.driver";
		public static final String URL = "log.url";
		public static final String USER = "log.user";
		public static final String PASSWORD = "log.password";
		public static final String SQL = "log.sql";
	}
	
	public static final class Default {
		public final static String DEFAULT_FORMAT = "%m%n%e";
		public final static Level DEFAULT_LEVEL = Level.INFO;
	}
	
	/** 日志名称 */
	private String name;
	
	/** 日志输出方式 */
	private Appender appender;
	
	/** 日志输出级别 */
	private Level level;
	
	private FormatParser formatParser;
	
	public Configuration() {
		this.appender = new ConsoleAppender();
		this.level = Default.DEFAULT_LEVEL;
		this.formatParser = new FormatParser(Default.DEFAULT_FORMAT);
	}
	
	public Configuration(String configFile) {
		Properties prop = new Properties();
		InputStream is = null;
		try {
			is = ResourceUtil.getInputStream(configFile);
			prop.load(is);
		} catch (Exception e) {
			System.err.println("Could not read configuration file from URL [" +
					configFile + "]. Expception:" + e.getMessage());
		} finally {
	        if (is != null) {
	        	try {
					is.close();
				} catch (Exception e) {
				}
	        }
		}
		doConfig(prop);
	}
	
	public Configuration(Properties prop) {
		doConfig(prop);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Appender getAppender() {
		return appender;
	}
	
	public Level getLevel() {
		return level;
	}
	
	public void setLevel(Level level) {
		this.level = level;
	}

	public void setLevel(String level) {
		this.level = Level.toLevel(level, Level.INFO);
	}

	public FormatParser getFormatParser() {
		return formatParser;
	}
	
	private void doConfig(Properties prop) {
		this.name = prop.getProperty(Prefix.NAME);
		String level = prop.getProperty(Prefix.LEVEL);
		if (level == null) {
			level = Level.INFO.getString();
		}
		this.level = Level.toLevel(level, Level.INFO);
		this.appender = parseAppender(prop);
		this.formatParser = parseFormatOrSql(prop);
	}
	
	private Appender parseAppender(Properties prop) {
		String appender = prop.getProperty(Prefix.APPENDER);
		
	    if (appender != null) {
	    	String s = appender.toLowerCase();
		    if (s.equals(Appender.CONSOLE)) {
		    	return new ConsoleAppender();
		    } else if (s.equals(Appender.FILE)) {
		    	String file = prop.getProperty(Prefix.FILE);
		    	return new FileAppender(file);
		    } else if (s.equals(Appender.DATABASE)) {
		    	String driver = prop.getProperty(Prefix.DRIVER);
		    	String url = prop.getProperty(Prefix.URL);
		    	String user = prop.getProperty(Prefix.USER);
		    	String password = prop.getProperty(Prefix.PASSWORD);
		    	String sql = prop.getProperty(Prefix.SQL);
		    	return new DatabaseAppender(driver, url, user, password, sql);
		    }
	    }
	    
	    System.err.println("Log:Unexpected appender [" + appender + 
	    		"], using default appender[" + Appender.CONSOLE + "]");
	    return new ConsoleAppender();
	}
	
	private FormatParser parseFormatOrSql(Properties prop) {
		String appender = prop.getProperty(Prefix.APPENDER);
		if (appender != null && appender.equalsIgnoreCase(Appender.DATABASE)) {
			String sql = prop.getProperty(Prefix.SQL);
			return new FormatParser(sql);
		} else {
			String format = prop.getProperty(Prefix.FORMAT);
			if (format == null) {
				format = Default.DEFAULT_FORMAT;
			}
			return new FormatParser(format);
		}
	}
}
