package cloud.apposs.logger;

/**
 * 日志输出级别
 */
public class Level implements Comparable<Level> {
	final static public Level OFF = new Level(0, "off");
	final static public Level FATAL = new Level(1, "fatal");
	final static public Level ERROR = new Level(2, "error");
	final static public Level WARN  = new Level(3, "warn");
	final static public Level INFO  = new Level(4, "info");
	final static public Level DEBUG = new Level(5, "debug");
	final static public Level TRACE = new Level(6, "trace");
	
	private int value;
	private String string;
	
	public Level(int value, String string) {
		this.value = value;
	    this.string = string;
	}
	
	public int getValue() {
		return value;
	}

	public String getString() {
		return string;
	}
	
	public static Level toLevel(String levelStr, Level defaultLevel) {
	    if(levelStr == null) {
	       return defaultLevel;
	    }

	    String s = levelStr.toLowerCase();
	    if(s.equals(Level.TRACE.getString())) return Level.TRACE;
	    if(s.equals(Level.DEBUG.getString())) return Level.DEBUG; 
	    if(s.equals(Level.INFO.getString()))  return Level.INFO;
	    if(s.equals(Level.WARN.getString()))  return Level.WARN;  
	    if(s.equals(Level.ERROR.getString())) return Level.ERROR;
	    if(s.equals(Level.FATAL.getString())) return Level.FATAL;
	    if(s.equals(Level.OFF.getString())) return Level.OFF;
	    
	    System.err.println("Log:Unexpected level [" + levelStr + "], using default level[" + defaultLevel.getString() + "]");
	    return defaultLevel;
	  }

	@Override
	public int compareTo(Level otherLevel) {
		return this.value - otherLevel.getValue();
	}
}
