package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;

import java.lang.reflect.Method;

/**
 * 对 {@link cloud.apposs.netkit.server.fai.FaiHandler} 里的注解进行解析
 */
public interface HandlerAnnotationParser {

    /**
     * 解析 {@link FaiHandler} 类上的注解
     */
    public void parse(FaiHandler handler);

    /**
     * 解析 {@link FaiHandler} 类里方法的注解
     */
    public void parseMethod(FaiHandler handler, Method method);
}
