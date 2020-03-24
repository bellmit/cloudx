package cloud.apposs.util;

public final class ClassUtil {
	private static final char PACKAGE_SEPARATOR = '.';
	public static final String CLASS_FILE_SUFFIX = ".class";
	
	/**
     * 获取类加载器
     */
    public static final ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
	
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return loadClass(className, true);
    }
    
    public static Class<?> loadClass(String className, 
    		boolean isInitialized) throws ClassNotFoundException {
        return Class.forName(className, isInitialized, getClassLoader());
    }

    /**
     * 加载类
     */
    public static Class<?> loadClass(String className, 
    		boolean isInitialized, ClassLoader classLoader) throws ClassNotFoundException {
    	if (StrUtil.isEmpty(className) || classLoader == null) {
    		return null;
    	}
        return Class.forName(className, isInitialized, classLoader);
    }
    
    public static String getClassFileName(Class<?> clazz) {
		SysUtil.checkNotNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}
}
