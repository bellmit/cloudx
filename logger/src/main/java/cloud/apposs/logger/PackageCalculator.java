package cloud.apposs.logger;

import java.io.Serializable;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 包类分析器，负责分析每个类的包路径和包版本等
 */
public final class PackageCalculator {
    private static final String PACKAGE_CALCULATE_NOT_FOUND = "NP";
    private static final String CRLF = System.getProperty("line.separator");

    /**
     * 类包查询缓存，避免重复反射获取导致性能下降
     */
    private final Map<String, ClassPackageData> packageCache = new HashMap<String, ClassPackageData>();

    /**
     * 打印堆栈信息
     *
     * @param elements 异常堆栈
     */
    public String printStraceTraceFrames(StackTraceElement[] elements) {
        if (elements == null) {
            return null;
        }

        try {
            StringBuilder builder = new StringBuilder(256);
            for (int i = 0; i < elements.length; i++) {
                StackTraceElement element = elements[i];
                ClassPackageData packageData = calculateByExactType(Class.forName(element.getClassName()));
                builder.append("\tat ").append(element).append(" [").append(packageData).append("]").append(CRLF);
            }
            return builder.toString();
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 获取堆栈信息
     *
     * @param elements 异常堆栈
     */
    public String[] getStraceTraceFrames(StackTraceElement[] elements) {
        if (elements == null) {
            return null;
        }

        try {
            String[] straceTraceFrames = new String[elements.length];
            StringBuilder builder = new StringBuilder(32);
            for (int i = 0; i < elements.length; i++) {
                StackTraceElement element = elements[i];
                ClassPackageData packageData = calculateByExactType(Class.forName(element.getClassName()));
                builder.setLength(0);
                builder.append(element).append(" [").append(packageData).append("]").append(CRLF);
                straceTraceFrames[i] = builder.toString();
            }
            return straceTraceFrames;
        } catch (Exception e) {
        }
        return null;
    }

    private ClassPackageData calculateByExactType(Class type) {
        String className = type.getName();
        ClassPackageData cpd = packageCache.get(className);
        if (cpd != null) {
            return cpd;
        }
        String version = getImplementationVersion(type);
        String codeLocation = getClassLocation(type);
        cpd = new ClassPackageData(codeLocation, version);
        packageCache.put(className, cpd);
        return cpd;
    }

    /**
     * 获取类所在包路径
     *
     * @param  clazz 包类
     * @return 类所在包路径，如果找不到则返回{@link #PACKAGE_CALCULATE_NOT_FOUND}
     */
    public String getClassLocation(Class clazz) {
        if (clazz == null) {
            return PACKAGE_CALCULATE_NOT_FOUND;
        }

        try {
            // file:/C:/java/maven-2.0.8/repo/com/icegreen/greenmail/1.3/greenmail-1.3.jar
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                URL resource = codeSource.getLocation();
                if (resource != null) {
                    String locationStr = resource.toString();
                    // now lets remove all but the file name
                    String result = doGetClassLocation(locationStr, '/');
                    if (result != null) {
                        return result;
                    }
                    return doGetClassLocation(locationStr, '\\');
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return PACKAGE_CALCULATE_NOT_FOUND;
    }

    /**
     * 获取类版本号，
     * 主要是用于包编译后的版本号，类包在当着项目下是没有版本的
     *
     * @param  clazz 包类
     * @return 类版本号，如果找不到则返回{@link #PACKAGE_CALCULATE_NOT_FOUND}
     */
    public String getImplementationVersion(Class clazz) {
        if (clazz == null) {
            return PACKAGE_CALCULATE_NOT_FOUND;
        }

        Package aPackage = clazz.getPackage();
        if (aPackage != null) {
            String v = aPackage.getImplementationVersion();
            if (v == null) {
                return PACKAGE_CALCULATE_NOT_FOUND;
            } else {
                return v;
            }
        }

        return PACKAGE_CALCULATE_NOT_FOUND;
    }

    private String doGetClassLocation(String locationStr, char separator) {
        int idx = locationStr.lastIndexOf(separator);
        if (isFolder(idx, locationStr)) {
            idx = locationStr.lastIndexOf(separator, idx - 1);
            return locationStr.substring(idx + 1);
        } else if (idx > 0) {
            return locationStr.substring(idx + 1);
        }
        return null;
    }

    private boolean isFolder(int idx, String text) {
        return (idx != -1 && idx + 1 == text.length());
    }

    public static class ClassPackageData implements Serializable {
        private static final long serialVersionUID = 504643281218337001L;

        /**
         * 包类所在物理路径
         */
        private final String location;

        /**
         * 类包版本
         */
        private final String version;

        public ClassPackageData(String location, String version) {
            this.location = location;
            this.version = version;
        }

        public String getLocation() {
            return location;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return location + ":" + version;
        }
    }
}
