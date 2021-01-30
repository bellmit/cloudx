package cloud.apposs.gateway.configure;

import java.util.LinkedList;
import java.util.List;

/**
 * 配置文件每个块/行的解析
 */
public final class Block {
    /** 当前块配置项名称 */
    private final String key;

    /** 块配置项名后面的参数，可为空，示例：location /中的location */
    private final List<String> arguments = new LinkedList<String>();

    /**
     * 当前是否为块配置，
     * 块配置为{XXXX}，非块配置为XXXX;
     */
    private final boolean isBlock;
    /**
     * 块配置值，仅为isBlock为true时
     */
    private final List<Block> values;

    /** 当前块配置起始行数 */
    private final int lineNo;

    public Block(String key, boolean isBlock, int lineNo) {
        this.key = key;
        this.isBlock = isBlock;
        if (!isBlock) {
            this.values = null;
        } else {
            this.values = new LinkedList<Block>();
        }
        this.lineNo = lineNo;
    }

    public String getKey() {
        return key;
    }

    public boolean isBlock() {
        return isBlock;
    }

    public void addArgument(String argument) {
        arguments.add(argument);
    }

    public void addArguments(List<String> arguments) {
        this.arguments.addAll(arguments);
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void addValues(List<Block> values) {
        this.values.addAll(values);
    }

    public List<Block> getValues() {
        return values;
    }

    public int getLineNo() {
        return lineNo;
    }
}
