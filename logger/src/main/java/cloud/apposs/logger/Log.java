package cloud.apposs.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 日志
 */
public class Log {
    private final static String LOG_THREAD_NAME = "Log_Thread";

    /**
     * 日志消息队列
     */
    private LinkedBlockingQueue<LogInfo> messageQueue;

    /**
     * 日志配置文件
     */
    private Configuration configuration;

    /**
     * 方法参数占位符，避免方法重载编译器无法区分
     */
    protected static final Throwable EMPTY_THROWABLE = new Throwable();

    /**
     * 日志线程，负责异步从日志消息队列中取出消息并输出/写入文件/写入数据库
     */
    private class LogThread implements Runnable {
        public LogThread() {
        }

        @Override
        public void run() {
            try {
                Appender appender = configuration.getAppender();
                FormatParser parser = configuration.getFormatParser();
                for (; ; ) {
                    // 阻塞等待直到有数据
                    LogInfo info = messageQueue.take();
                    List<String> msgList = new ArrayList<String>();
                    if (info != null) {
                        msgList.add(parser.format(info));
                    }

                    for (int i = 0; i < messageQueue.size(); i++) {
                        info = messageQueue.poll();
                        if (info != null) {
                            msgList.add(parser.format(info));
                        }
                    }

                    appender.append(msgList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Log() {
        messageQueue = new LinkedBlockingQueue<LogInfo>();
        configuration = new Configuration();

        startThread();
    }

    public Log(String configFile) {
        messageQueue = new LinkedBlockingQueue<LogInfo>();
        configuration = new Configuration(configFile);

        startThread();
    }

    public Log(Properties prop) {
        messageQueue = new LinkedBlockingQueue<LogInfo>();
        configuration = new Configuration(prop);

        startThread();
    }

    private void startThread() {
        Thread logThread = new Thread(new LogThread(), LOG_THREAD_NAME);
        logThread.setDaemon(true);
        logThread.start();
    }

    public void trace(String message) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void trace(Throwable throwable) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public void trace(String message, Object... args) {
        log(Level.TRACE, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void trace(Throwable throwable, String message, Object... args) {
        log(Level.TRACE, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void debug(String message) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void debug(Throwable throwable) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), 0, null);
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void debug(Throwable throwable, String message, Object... args) {
        log(Level.DEBUG, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void info(String message) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void info(Throwable throwable) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), 0, "");
    }

    public void info(String message, Object... args) {
        log(Level.INFO, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void info(Throwable throwable, String message, Object... args) {
        log(Level.INFO, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void warn(String message) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void warn(Throwable throwable) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), 0, "");
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void warn(Throwable throwable, String message, Object... args) {
        log(Level.WARN, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void error(String message) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void error(Throwable throwable) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), 0, "");
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void error(Throwable throwable, String message, Object... args) {
        log(Level.ERROR, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void fatal(String message) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message);
    }

    public void fatal(Throwable throwable) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), 0, "");
    }

    public void fatal(String message, Object... args) {
        log(Level.FATAL, Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void fatal(Throwable throwable, String message, Object... args) {
        log(Level.FATAL, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void log(String level, String message, Object... args) {
        log(Level.toLevel(level, Level.INFO), Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void log(String level, int errno, String message, Object... args) {
        log(Level.toLevel(level, Level.INFO), Log.EMPTY_THROWABLE, new LocationInfo(new Throwable()), errno, message, args);
    }

    public void log(String level, Throwable throwable, String message, Object... args) {
        log(Level.toLevel(level, Level.INFO), throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void log(Level level, Throwable throwable, String message, Object... args) {
        log(level, throwable, new LocationInfo(new Throwable()), 0, message, args);
    }

    public void log(Level level, Throwable throwable,
                    LocationInfo locationInfo, int errno, String message, Object... args) {
        Level threshold = configuration.getLevel();
        if (level.compareTo(threshold) > 0) {
            return;
        }

        String msg = null;
        if (message != null) {
            try {
                msg = String.format(message, args);
            } catch (Exception e) {
                msg = "message[" + message + "]format error";
            }
        }
        String logName = configuration.getName();
        String threadName = Thread.currentThread().getName();
        LogInfo info = new LogInfo(level, logName, msg, throwable, locationInfo, threadName, errno);
        messageQueue.add(info);
    }

    public boolean isDebugEnabled() {
        return isLevelEnable(Level.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isLevelEnable(Level.INFO);
    }

    public boolean isLevelEnable(Level level) {
        Level threshold = configuration.getLevel();
        return level.compareTo(threshold) <= 0;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void close() {
        if (configuration.getAppender() != null) {
            configuration.getAppender().close();
        }
    }
}
