package cloud.apposs.rest;

import cloud.apposs.rest.annotation.Request.Method;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * 封装{@link cloud.apposs.rest.annotation.Request}对象相关信息
 */
public final class Requestor {
    private final String host;

    private final String[] path;

    private final Annotation annotation;

    private final Method[] methods;

    public Requestor(String host, String[] path, Annotation annotation, Method... methods) {
        this.host = host;
        this.path = path;
        this.annotation = annotation;
        this.methods = methods;
    }

    public String getHost() {
        return host;
    }

    public String[] getPath() {
        return path;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Method[] getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return "Requestor{" +
                "host='" + host + '\'' +
                ", path=" + Arrays.toString(path) +
                ", annotation=" + annotation +
                ", methods=" + Arrays.toString(methods) +
                '}';
    }
}
