package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.util.SysUtil;

/**
 * 解析器链提供器
 */
public class ParserChainProvider {

    private static ParserChainBuilder builder;

    /**
     * 获取新解析器链
     */
    public static ParserChain newParserChain(FaiHandler handler) {
        SysUtil.checkNotNull(handler, "handler null");
        if (builder == null) {
            builder = getDeafultBuilder();
        }
        return builder.build(handler);
    }

    public static void setBuilder(ParserChainBuilder builder) {
        ParserChainProvider.builder = builder;
    }

    public static ParserChainBuilder getDeafultBuilder() {
        return new DefaultParserChainBuilder();
    }
}
