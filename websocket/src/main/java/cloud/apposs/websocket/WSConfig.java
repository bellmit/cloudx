package cloud.apposs.websocket;

import cloud.apposs.netkit.server.websocket.WebSocketConfig;

public class WSConfig extends WebSocketConfig {
    /**
     * 扫描基础包，必须配置，框架会自动扫描ServerEndpoint注解类
     */
    protected String basePackage;

    /**
     * 是否输出系统信息
     */
    protected boolean showSysInfo = true;

    /**
     * 业务自定义配置
     */
    protected Object options;

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public boolean isShowSysInfo() {
        return showSysInfo;
    }

    public void setShowSysInfo(boolean showSysInfo) {
        this.showSysInfo = showSysInfo;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }
}
