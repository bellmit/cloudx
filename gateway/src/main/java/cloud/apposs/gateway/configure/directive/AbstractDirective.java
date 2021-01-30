package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.BlockDirective;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.util.StrUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 指令解析抽象，因为是属于启动时的指令解析，所以对性能要求不高
 */
public abstract class AbstractDirective implements BlockDirective.IBlockDirective {
    /**
     * 检查块配置是否为块并且参数必须为空
     */
    protected void checkBlockArgumentMustEmpty(Block block) throws ConfigParseException {
        if (!block.isBlock() || !block.getArguments().isEmpty()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
    }

    /**
     * 获取非块配置第一个参数
     */
    protected String getNonBlockArgumentOne(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        if (arguments.size() != 1 || StrUtil.isEmpty(arguments.get(0))) {
            throw new ConfigParseException(block.getLineNo(), "invalid number of arguments in \""+ block.getKey() + "\" directive");
        }
        return arguments.get(0);
    }

    /**
     * 获取块配置第一个参数
     */
    protected String getBlockArgumentOne(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (!block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        if (arguments.size() != 1 || StrUtil.isEmpty(arguments.get(0))) {
            throw new ConfigParseException(block.getLineNo(), "invalid number of arguments in \""+ block.getKey() + "\" directive");
        }
        return arguments.get(0);
    }

    /**
     * 获取非块配置参数列表
     */
    protected List<String> getNonBlockArgumentTwo(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        if (arguments.size() != 2) {
            throw new ConfigParseException(block.getLineNo(), "invalid number of arguments in \""+ block.getKey() + "\" directive");
        }
        return arguments;
    }

    /**
     * 获取非块配置参数列表
     */
    protected List<String> getNonBlockArgumentListTwo(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        if (arguments.size() < 2) {
            throw new ConfigParseException(block.getLineNo(), "invalid number of arguments in \""+ block.getKey() + "\" directive");
        }
        List<String> zipArgs = new LinkedList<String>();
        StringBuilder argumentStr = new StringBuilder();
        for (int i = 1; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (i == 1) {
                argument = argument.trim();
            }
            argumentStr.append(argument);
        }
        zipArgs.add(arguments.get(0));
        zipArgs.add(argumentStr.toString());
        return zipArgs;
    }

    /**
     * 获取非块配置所有参数并拼接成字符串
     */
    protected String getNonBlockArgumentListString(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        StringBuilder argumentStr = new StringBuilder();
        for (String argument : arguments) {
            argumentStr.append(argument);
        }
        return argumentStr.toString();
    }

    /**
     * 获取非块配置所有参数并拼接成字符串
     */
    protected String getNonBlockArgumentListStringTwo(Block block) throws ConfigParseException {
        List<String> arguments = block.getArguments();
        if (block.isBlock()) {
            throw new ConfigParseException(block.getLineNo(), "\"" + block.getKey() + "\" directive invalid");
        }
        if (arguments.size() < 2) {
            throw new ConfigParseException(block.getLineNo(), "invalid number of arguments in \""+ block.getKey() + "\" directive");
        }
        StringBuilder argumentStr = new StringBuilder();
        for (int i = 1; i < arguments.size(); i++) {
            argumentStr.append(arguments.get(i));
        }
        return argumentStr.toString();
    }
}
