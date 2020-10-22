package cloud.apposs.util;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Properties;

public final class SystemInfo {
    private static SystemInfo instance = new SystemInfo();

    private final Properties sysProps = System.getProperties();

    private SystemInfo() {
    }

    public static SystemInfo getInstance() {
        return instance;
    }

    /**
     * 获取操作系统名称
     */
    public String getOsName() {
        return sysProps.getProperty("os.name");
    }

    public String getOsArch() {
        return sysProps.getProperty("os.arch");
    }

    public String getOsVersion() {
        return sysProps.getProperty("os.version");
    }

    public String getJavaHome() {
        return sysProps.getProperty("java.home");
    }

    public String getJavaVersion() {
        return sysProps.getProperty("java.vm.specification.version");
    }

    public String getJavaVendor() {
        return sysProps.getProperty("java.vm.specification.vendor");
    }

    public List<String> getJvmArguments() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }
}
