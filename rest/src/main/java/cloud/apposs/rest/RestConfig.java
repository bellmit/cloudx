package cloud.apposs.rest;

import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.StrUtil;

public class RestConfig {
    public static final String DEFAULT_CHARSET = "utf-8";

    private String charset = DEFAULT_CHARSET;

    /**
     * 扫描基础包，必须配置，框架会自动扫描Action注解类
     */
    private String basePackage;

    private Object attachment;

    /**
     * 当前服务是否为只读，
     * 当开启只读模式时，所有Action中注解WriteCmd的指令均不再响应请求，直接抛出ReadOnlyException，
     * 这种一般只应用于线上服务数据迁移场景，保证用户可以展现页面但无法编辑页面
     */
    private boolean readOnly = false;

    /**
     * 是否开启权限注解拦截验证
     */
    private boolean authDriven = false;

    /**
     * 是否输出请求日志
     */
    protected boolean httpLogEnable = true;

    /**
     * 请求日志输出格式
     */
    protected String httpLogFormat =
            "$method $host $uri $status $action.$handler $remote_addr:$remote_port $attr_errno $attr_flow $time(ms)";

    public RestConfig() {
    }

    public RestConfig(Class<?> primarySource) {
        this.basePackage = ReflectUtil.getPackage(primarySource);
    }

    /**
     * 框架过滤不拦截的URL
     */
    protected String excludeUrlPattern;
    protected String[] excludeUrlPatterns;

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public String getExcludeUrlPattern() {
        return excludeUrlPattern;
    }

    public void setExcludeUrlPattern(String excludeUrlPattern) {
        this.excludeUrlPattern = excludeUrlPattern;
    }

    public String[] getExcludeUrlPatterns() {
        return excludeUrlPatterns;
    }

    public void setExcludeUrlPatterns(String[] excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isAuthDriven() {
        return authDriven;
    }

    public void setAuthDriven(boolean authDriven) {
        this.authDriven = authDriven;
    }

    public boolean isHttpLogEnable() {
        return httpLogEnable;
    }

    public void setHttpLogEnable(boolean httpLogEnable) {
        this.httpLogEnable = httpLogEnable;
    }

    public String getHttpLogFormat() {
        return httpLogFormat;
    }

    public void setHttpLogFormat(String httpLogFormat) {
        if (!StrUtil.isEmpty(httpLogFormat)) {
            this.httpLogFormat = httpLogFormat;
        }
    }
}
