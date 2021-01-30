package cloud.apposs.rest;

import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.parameter.Parameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 封装{@link cloud.apposs.rest.annotation.Action}注解类方法相关信息，
 * 每个Action中的Method代表一个Handler实例，全局单例
 */
public class Handler {
    /**
     * Class类
     */
    private final Class<?> clazz;

    /**
     * 方法反射
     */
    private final Method method;

    /**
     * 方法参数，包括参数类型
     */
    private final Parameter[] parameters;

    /**
     * 请求的匹配方法列表，如果为空则匹配所有请求方法
     */
    private Request.Method[] methods;

    /**
     * 请求的匹配主机
     */
    private String host;

    /**
     * 请求的匹配路径
     */
    private String path;

    /**
     * 方法请求路径是否是正则表达式
     */
    private boolean pattern = false;

    /**
     * 是否是异步请求处理，
     * 当Handler方法中被标注为@Async或者返回值为RxIo时才是异步
     */
    private boolean async = false;

    /**
     * 是否为写指令，
     * 当配置开启只读模式时，该Handler则不再响应请求，直接抛出ReadOnlyException，
     * 这种一般只应用于线上服务数据迁移场景，保证用户可以展现页面但无法编辑页面
     */
    private boolean writeCmd = false;

    /**
     * 是否开启了熔断服务
     */
    private boolean guard = false;
    private String resource;

    /**
     * 方法注解，
     * 业务方可在{@link cloud.apposs.rest.annotation.Action}类添加自定义注解，并通过Handler来解析自定义注解
     */
    private final Map<Class<? extends Annotation>, Annotation> annotations;

    /**
     * 自定义附件，可由业务自由定义
     */
    private Object attachment;

    public Handler(Class<?> clazz, Method method, Parameter[] parameters) {
        this.clazz = clazz;
        this.method = method;
        this.parameters = parameters;
        Annotation[] annotations = method.getAnnotations();
        this.annotations = new ConcurrentHashMap<Class<? extends Annotation>, Annotation>(annotations.length);
        for (int i = 0; i < annotations.length; i++) {
            Annotation annotation = annotations[i];
            this.annotations.put(annotation.annotationType(), annotation);
        }
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isPattern() {
        return pattern;
    }

    public Handler setPattern(boolean pattern) {
        this.pattern = pattern;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public Handler setAsync(boolean async) {
        this.async = async;
        return this;
    }

    public boolean isWriteCmd() {
        return writeCmd;
    }

    public Handler setWriteCmd(boolean writeCmd) {
        this.writeCmd = writeCmd;
        return this;
    }

    public boolean isGuard() {
        return guard;
    }

    public String getResource() {
        return resource;
    }

    public Handler setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public Handler setGuard(boolean guard) {
        this.guard = guard;
        return this;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public String getHost() {
        return host;
    }

    public Handler setHost(String host) {
        this.host = host;
        return this;
    }

    public Request.Method[] getMethods() {
        return methods;
    }

    public Handler setMethods(Request.Method[] methods) {
        this.methods = methods;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Handler setPath(String path) {
        this.path = path;
        return this;
    }

    public Object getAttachment() {
        return attachment;
    }

    public Handler setAttachment(Object attachment) {
        this.attachment = attachment;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return (T) annotations.get(annotationType);
    }

    public <T extends Annotation> boolean hasAnnotation(Class<T> annotationType) {
        return annotations.containsKey(annotationType);
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder(128);
        info.append("{");
        info.append("Bean: ").append(clazz.getSimpleName()).append(", ");
        String methodStr = methods.length > 0 ? "" : "*";
        for (int i = 0; i < methods.length; i++) {
            Request.Method method = methods[i];
            if (i < methods.length - 1) {
                methodStr += method + ",";
            } else {
                methodStr += method;
            }
        }
        info.append("Method: [").append(methodStr).append("], ");
        info.append("Path: ").append(path);
        info.append("}");
        return info.toString();
    }
}