package cloud.apposs.logger;

/**
 * 类的详细信息
 */
public class LocationInfo {
	private int lineNumber;
	
	private String fileName;
	
	private String className;
	
	private String methodName;
	
	public LocationInfo(Throwable throwable) {
		StackTraceElement[] stacks = throwable.getStackTrace();
		StackTraceElement element = stacks[1];
		this.fileName = element.getFileName();
		this.className = element.getClassName();
		this.methodName = element.getMethodName();
		this.lineNumber = element.getLineNumber();
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getFileName() {
		return fileName;
	}

	public String getClassName() {
		return className;
	}

	public String getMethodName() {
		return methodName;
	}
}
