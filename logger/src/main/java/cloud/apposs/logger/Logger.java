package cloud.apposs.logger;

import java.util.Properties;

/**
 * 日志，所有方法静态并保证全局调用只有一个Log类
 *
 * @author wayken.hong@gmail.com
 * @date 2013.04.30
 */
public class Logger {
    private static Log log;

    public static void config(String configFile) {
        log = new Log(configFile);
    }

    public static void config(Properties prop) {
        log = new Log(prop);
    }

    public static void trace(String message) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void trace(int errno, String message) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void trace(Throwable throwable) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void trace(int errno, Throwable throwable) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void trace(String message, Object... args) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void trace(int errno, String message, Object... args) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void trace(Throwable throwable, String message, Object... args) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void trace(int errno, Throwable throwable, String message, Object... args) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void debug(String message) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void debug(int errno, String message) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void debug(Throwable throwable) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void debug(int errno, Throwable throwable) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void debug(String message, Object... args) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void debug(int errno, String message, Object... args) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void debug(Throwable throwable, String message, Object... args) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void debug(int errno, Throwable throwable, String message, Object... args) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void info(String message) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void info(int errno, String message) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void info(Throwable throwable) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void info(int errno, Throwable throwable) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void info(int errno, String message, Object... args) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void info(Throwable throwable, String message, Object... args) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void info(int errno, Throwable throwable, String message, Object... args) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void warn(String message) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void warn(int errno, String message) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void warn(Throwable throwable) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void warn(int errno, Throwable throwable) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void warn(String message, Object... args) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void warn(int errno, String message, Object... args) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void warn(Throwable throwable, String message, Object... args) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void warn(int errno, Throwable throwable, String message, Object... args) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void error(String message) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void error(int errno, String message) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void error(Throwable throwable) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void error(int errno, Throwable throwable) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void error(String message, Object... args) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void error(int errno, String message, Object... args) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void error(Throwable throwable, String message, Object... args) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void error(int errno, Throwable throwable, String message, Object... args) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void fatal(String message) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public static void fatal(int errno, String message) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message);
    }

    public static void fatal(Throwable throwable) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public static void fatal(int errno, Throwable throwable) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), errno, null);
    }

    public static void fatal(String message, Object... args) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void fatal(int errno, String message, Object... args) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void fatal(Throwable throwable, String message, Object... args) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void fatal(int errno, Throwable throwable, String message, Object... args) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void log(Level level, String message, Object... args) {
        log(level, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void log(Level level, int errno, String message, Object... args) {
        log(level, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void log(Level level, Throwable throwable, int errno, String message, Object... args) {
        log(level, throwable, new LocationInfo(new Throwable()), errno, message, args);
    }

    public static void log(Level level, Throwable throwable, String message, Object... args) {
        log(level, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public static void log(Level level, Throwable throwable,
                           LocationInfo locationInfo, int errno, String message, Object... args) {
        checkInitialized();
        log.log(level, throwable, locationInfo, errno, message, args);
    }

    public static boolean isDebugEnabled() {
        return isLevelEnable(Level.DEBUG);
    }

    public static boolean isInfoEnabled() {
        return isLevelEnable(Level.INFO);
    }

    public static boolean isLevelEnable(Level level) {
        checkInitialized();
        return log.isLevelEnable(level);
    }

    public static Configuration getConfiguration() {
        checkInitialized();
        return log.getConfiguration();
    }

    public static void close() {
        if (log != null) {
            log.close();
        }
    }

    private static void checkInitialized() {
        if (log == null) {
            log = new Log();
        }
    }
}
