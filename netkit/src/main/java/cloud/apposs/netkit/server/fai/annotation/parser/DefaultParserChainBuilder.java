package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.FaiHandler;

/**
 * 默认解析器链建造者
 */
public class DefaultParserChainBuilder implements ParserChainBuilder {

    @Override
    public ParserChain build(FaiHandler handler) {
        ParserChain chain = getDefaultChain(handler);
        chain.addLast(new CmdParser());
        chain.addLast(new GuardCmdParser());
        chain.addLast(new WrittenCmdParser());
        chain.addLast(new ReadenCmdParser());
        chain.addLast(new CmdTaskGroupParser());
        return chain;
    }

    /**
     * 获取默认解析器链
     */
    private ParserChain getDefaultChain(FaiHandler handler) {
        return new LinkedParserChain(handler);
    }
}
