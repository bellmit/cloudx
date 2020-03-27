package cloud.apposs.logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * 日志信息
 */
public class LogInfo {
	private Level level;
	
	private String name;

	private String message;
	
	private Throwable throwable;
	
	private LocationInfo locationInfo;
	
	private long timeStamp;
	
	private String threadName;
	
	private long threadId = -1;
	
	private int errno = 0;
	
	public LogInfo(Level level, String name, String message, Throwable throwable,
			LocationInfo locationInfo, String threadName, int errno) {
		this.level = level;
		this.name = name;
		this.message = message;
		if (throwable != Log.EMPTY_THROWABLE) {
			this.throwable = throwable;
		}
		this.locationInfo = locationInfo;
		this.threadName = threadName;
		this.timeStamp = System.currentTimeMillis();
		this.errno = errno;
	}
	
	public Level getLevel() {
		return level;
	}
	
	public String getName() {
		return name;
	}

	public String getMessage() {
		return message;
	}
	
	public Throwable getThrowable() {
		return throwable;
	}
	
	public LocationInfo getLocationInfo() {
		if (locationInfo == null) {
			locationInfo = new LocationInfo(new Throwable());
		}
		return locationInfo;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public String getThreadName() {
		if (threadName == null) {
			threadName = (Thread.currentThread()).getName();
		}
		return threadName;
	}
	
	public long getThreadId() {
		if (threadId == -1) {
			threadId = (Thread.currentThread()).getId();
		}
		return threadId;
	}

	public int getErrno() {
		return errno;
	}

	public String[] getThrowableInfo() {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            throwable.printStackTrace(pw);
        } catch(RuntimeException ex) {
        }
        pw.flush();
        LineNumberReader reader = new LineNumberReader(
                new StringReader(sw.toString()));
        ArrayList<String> lines = new ArrayList<String>();
        try {
          String line = reader.readLine();
          while(line != null) {
            lines.add(line);
            line = reader.readLine();
          }
        } catch(IOException ex) {
            if (ex instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            lines.add(ex.toString());
        }
        String[] tempRep = new String[lines.size()];
        lines.toArray(tempRep);
        return tempRep;
	}
}
