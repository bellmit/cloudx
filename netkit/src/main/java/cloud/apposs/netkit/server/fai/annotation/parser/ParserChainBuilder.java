package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;

/**
 * 解析器链建造器
 */
public interface ParserChainBuilder {
    public ParserChain build(FaiHandler handler);
}
