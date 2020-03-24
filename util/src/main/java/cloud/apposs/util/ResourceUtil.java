package cloud.apposs.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class ResourceUtil {
    public static final String URL_PROTOCOL_FILE = "file";
    public static final String URL_PROTOCOL_WAR = "war";
    public static final String URL_PROTOCOL_ZIP = "zip";
    public static final String URL_PROTOCOL_JAR = "jar";

    private static final char PACKAGE_SEPARATOR = '.';
    private static final char PATH_SEPARATOR = '/';

    public static InputStream getResource(final String path) throws IOException {
        return getResource(path, ResourceUtil.class);
    }

    public static InputStream getResource(final String path, final Class<?> clazz) throws IOException {
        InputStream is = null;

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
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

    /**
     * 从Properties中加载对象各种属性配置
     *
     * @param obj
     * @param props
     */
    public static void loadProperties(Object obj, Properties props) {
        try {
            setProperties(obj, props);
        } catch (Exception e) {
        }
    }

    public static void setProperties(Object obj, Properties props) throws Exception {
        for (Method method : obj.getClass().getDeclaredMethods()) {
            String tmp = null;
            if (method.getName().startsWith("is")) {
                tmp = lowerFirst(method.getName().substring(2));
            } else if (method.getName().startsWith("set")) {
                tmp = lowerFirst(method.getName().substring(3));
            } else {
                continue;
            }

            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(int.class)) {
                String val = props.getProperty(tmp);
                if (val != null) {
                    try {
                        method.invoke(obj, Integer.parseInt(val));
                    } catch (NumberFormatException e) {
                        // do nothing, use the default value
                    }
                }
            } else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(long.class)) {
                String val = props.getProperty(tmp);
                if (val != null) {
                    try {
                        method.invoke(obj, Long.parseLong(val));
                    } catch (NumberFormatException e) {
                        // do nothing, use the default value
                    }
                }
            } else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class)) {
                String val = props.getProperty(tmp);
                if (val != null) {
                    method.invoke(obj, val);
                }
            }
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(boolean.class)) {
                String val = props.getProperty(tmp);
                if (val != null) {
                    method.invoke(obj, Boolean.parseBoolean(val));
                }
            }
        }
    }

    public static String lowerFirst(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    /**
     * 判断资源链接是否为Jar包
     */
    public static boolean isJarURL(URL url) {
        String protocol = url.getProtocol();
        return (URL_PROTOCOL_JAR.equals(protocol) ||
                URL_PROTOCOL_WAR.equals(protocol) ||
                URL_PROTOCOL_ZIP.equals(protocol));
    }

    /**
     * 根据包名获取资源路径
     *
     * @param packagePath 包名，示例：com.java.util.*
     */
    public static Enumeration<URL> getResources(String packagePath) throws IOException {
        if (StrUtil.isEmpty(packagePath)) {
            throw new IllegalArgumentException("packageSearchPath");
        }
        packagePath = packagePath.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
        return ClassUtil.getClassLoader().getResources(packagePath);
    }

    public static String convertResourcePath(String className) {
        if (StrUtil.isEmpty(className)) {
            throw new IllegalArgumentException("className");
        }
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    /**
     * 获取当前类所在的路径
     */
    public static String getClassPath(Class<?> clazz, String path) {
        return clazz.getResource(path).getPath();
    }
}
