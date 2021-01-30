package cloud.apposs.gateway.configure;

import cloud.apposs.util.StrUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 配置文件解析模块
 */
public final class ConfigParser {
    public class State {
        /** 配置文件解析配置块开始状态 */
        public static final int PARSE_BLOCK_START = 1;
        public static final int PARSE_BLOCK_ARGS = 2;
        public static final int PARSE_QUOTES = 3;
        public static final int PARSE_COMMENT = 4;
    }

    /**
     * 解析网关配置文件
     *
     * @param content 配置文件内容，可从本地读取，也可从ZK注册中心读取
     */
    public static List<Block> parseConfig(String content) throws ConfigParseException {
        if (StrUtil.isEmpty(content)) {
            // 配置文件内容不允许为空
            throw new ConfigParseException(0, "no content section in configuration");
        }
        ParseContext context = new ParseContext();
        List<Block> blockList = doParseBlockList(content, 0, false, context);
        return blockList;
    }

    /**
     * 解析配置内容中的每个配置块并组合成列表，即
     * network {...},
     * log {...}
     */
    private static List<Block> doParseBlockList(String content,
                int index, boolean isRecursive, ParseContext context) throws ConfigParseException {
        List<Block> blockList = new LinkedList<Block>();
        int length = content.length();
        char letter;
        // 解析状态属性
        int status = State.PARSE_BLOCK_START;
        int lastStatus = State.PARSE_BLOCK_START;
        // 解析值临时内存
        StringBuffer currentKey = new StringBuffer(16);
        StringBuffer currentValue = new StringBuffer(32);
        List<String> arguments = new LinkedList<String>();
        // 解析的语法状态
        boolean isParseLineEnd = false;
        for (; index < length; index++) {
            letter = content.charAt(index);
            calcuateConfLine(context, content, letter, index);
            switch (status) {
                case State.PARSE_BLOCK_START:
                    // 去掉空格和换行符
                    if (isConfigSpaceCharactor(letter)) {
                        if (currentKey.length() > 0) {
                            // 块配置的Key已经解析结束，接下来解析块配置值
                            isParseLineEnd = false;
                            status = State.PARSE_BLOCK_ARGS;
                        }
                        continue;
                    }
                    if (letter == '#') {
                        // 解析为注释
                        status = State.PARSE_COMMENT;
                        lastStatus = State.PARSE_BLOCK_START;
                        continue;
                    }
                    if (letter == '}') {
                        isParseLineEnd = true;
                        // 解析到块结束，但却只有key没有value
                        if (!StrUtil.isEmpty(currentKey.toString())) {
                            throw new ConfigParseException(context.lineNo,
                                    "invalid arguments in \"" + currentKey.toString() + "\" directive");
                        }
                        if (isRecursive) {
                            return blockList;
                        }
                    }
                    // 配置Key必须是有效字符
                    if (!isCharactorValid(letter)) {
                        throw new ConfigParseException(context.lineNo, "invalid charactor \"" + letter + "\"");
                    }
                    currentKey.append(letter);
                    continue;
                case State.PARSE_BLOCK_ARGS:
                    // 解析到空格符
                    if (isConfigSpaceCharactor(letter)) {
                        if (currentValue.length() > 0) {
                            arguments.add(currentValue.toString());
                            currentValue.setLength(0);
                        }
                        continue;
                    }
                    // 解析为注释
                    if (letter == '#') {
                        status = State.PARSE_COMMENT;
                        lastStatus = State.PARSE_BLOCK_ARGS;
                        continue;
                    }
                    // 解析为一行文本
                    if (letter == '\'') {
                        status = State.PARSE_QUOTES;
                        continue;
                    }
                    if (letter == '}') {
                        // 解析到块结束，但却只有key没有value
                        if (!StrUtil.isEmpty(currentKey.toString())) {
                            throw new ConfigParseException(context.lineNo,
                                    "invalid arguments in \"" + currentKey.toString() + "\" directive");
                        }
                    }
                    // 解析到配置项结束符
                    if (letter == ';') {
                        Block block = new Block(currentKey.toString(), false, context.lineNo);
                        if (currentValue.length() > 0) {
                            arguments.add(currentValue.toString());
                        }
                        block.addArguments(arguments);
                        blockList.add(block);
                        // 重置状态
                        isParseLineEnd = true;
                        currentKey.setLength(0);
                        currentValue.setLength(0);
                        arguments.clear();
                        status = State.PARSE_BLOCK_START;
                        continue;
                    }
                    // 解析到块
                    if (letter == '{') {
                        int currentLineNo = context.lineNo;
                        List<Block> subBlockList = doParseBlockList(content, index + 1, true, context);
                        Block block = new Block(currentKey.toString(), true, currentLineNo);
                        if (!StrUtil.isEmpty(currentValue.toString())) {
                            arguments.add(currentValue.toString());
                        }
                        block.addArguments(arguments);
                        block.addValues(subBlockList);
                        blockList.add(block);
                        // 重置状态
                        isParseLineEnd = true;
                        index = context.eobIdx;
                        currentKey.setLength(0);
                        currentValue.setLength(0);
                        arguments.clear();
                        status = State.PARSE_BLOCK_START;
                        continue;
                    }
                    currentValue.append(letter);
                    continue;
                case State.PARSE_QUOTES:
                    if (letter == '\'') {
                        arguments.add(currentValue.toString());
                        currentValue.setLength(0);
                        status = State.PARSE_BLOCK_ARGS;
                        continue;
                    }
                    currentValue.append(letter);
                    continue;
                case State.PARSE_COMMENT:
                    // 去掉注释
                    if (letter == '\n') {
                        // 一行注释结束
                        status = lastStatus;
                    }
                    continue;
            }
        }
        if (!isParseLineEnd) {
            throw new ConfigParseException(context.lineNo, "expecting \"}\" or ;");
        }
        return blockList;
    }

    /**
     * 判断是否为Json空白字符
     */
    private static boolean isConfigSpaceCharactor(char letter) {
        return letter == ' ' || letter == '\t' || letter == '\n' || letter == '\r';
    }

    /**
     * 统计行数
     */
    private static void calcuateConfLine(ParseContext context, String json, char letter, int index) {
        context.eobIdx++;
        if (letter == '\n' && index > 0 && json.charAt(index - 1) == '\r') {
            // \r\n才算作一个换行符
            context.lineNo++;
        }
    }

    private static boolean isCharactorValid(char letter) {
        return (letter >= 'a' && letter <= 'z') || (letter >= 'A' && letter <= 'Z') || letter == '_' || letter == '-';
    }

    /**
     * Json解析过程中的各状态属性维护
     */
    private static final class ParseContext {
        /**
         * 解析conf文件所对应行数
         */
        public int lineNo = 1;

        /**
         * 结束块配置解析时的索引位
         */
        public int eobIdx = -1;
    }
}
