package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;

import java.lang.reflect.Method;

/**
 * 适配器，一些解析器只解析类上的注解，一些则只解析方法上的注解
 */
public class ParserAdaptor implements HandlerAnnotationParser {

    @Override
    public void parse(FaiHandler handler) {}

    @Override
    public void parseMethod(FaiHandler handler, Method method) {}
}
