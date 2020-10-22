package cloud.apposs.netkit.server.fai.annotation.parser;

import java.lang.reflect.Method;

/**
 * 解析器链
 */
public interface ParserChain {

    /**
     * 插在链最后
     */
    public ParserChain addLast(HandlerAnnotationParser parser);

    /**
     * 插在链头
     */
    public ParserChain addFirst(HandlerAnnotationParser parser);

    /**
     * 解析类注解
     */
    public void parse();

    /**
     * 解析方法注解
     */
    public void parseMethod(Method method);

}
