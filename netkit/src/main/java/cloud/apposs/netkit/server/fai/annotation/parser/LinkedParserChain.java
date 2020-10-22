package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.util.SysUtil;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * 链表解析器链
 */
public class LinkedParserChain implements ParserChain {

    private LinkedList<HandlerAnnotationParser> parsers = new LinkedList<HandlerAnnotationParser>();

    private FaiHandler handler;

    public LinkedParserChain(FaiHandler handler) {
        this.handler = handler;
    }

    @Override
    public ParserChain addLast(HandlerAnnotationParser parser) {
        SysUtil.checkNotNull(parser, "parser null");
        parsers.addLast(parser);
        return this;
    }

    @Override
    public ParserChain addFirst(HandlerAnnotationParser parser) {
        SysUtil.checkNotNull(parser, "parser null");
        parsers.addFirst(parser);
        return this;
    }

    @Override
    public void parse() {
        for (HandlerAnnotationParser parser : parsers) {
            parser.parse(handler);
        }
    }

    @Override
    public void parseMethod(Method method) {
        for (HandlerAnnotationParser parser : parsers) {
            parser.parseMethod(handler, method);
        }
    }
}
