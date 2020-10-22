package cloud.apposs.logger.formatter;

import cloud.apposs.logger.FormatInfo;
import cloud.apposs.logger.Formatter;
import cloud.apposs.logger.LogInfo;
import cloud.apposs.logger.PackageCalculator;

/**
 * 输出代码中指定异常消息，连同该异常类所在的包路径和包版本也打印出来，对应关键字[%E]
 * 内容如下：
 * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) [NP:1.8.0_202]
 * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) [NP:1.8.0_202]
 */
public class PackageTraceFormatter extends Formatter {
    private final PackageCalculator pkgCal = new PackageCalculator();

    public PackageTraceFormatter(FormatInfo formatInfo) {
        super(formatInfo);
    }

    @Override
    public String convert(LogInfo info) {
        Throwable throwable = info.getThrowable();
        if (throwable == null) {
            return null;
        }
        StackTraceElement[] strackTrace = throwable.getStackTrace();
        return pkgCal.printStraceTraceFrames(strackTrace);
    }
}
