package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.Directive;

import java.util.List;

public class LogDirective extends AbstractDirective {
    @Override
    public void parse(Block block, GatewayConfig config) throws ConfigParseException {
        checkBlockArgumentMustEmpty(block);
        List<Block> values = block.getValues();
        for (Block value : values) {
            String directive = value.getKey();
            switch (directive) {
                case Directive.LOG_LEVEL:
                    config.setLogLevel(getNonBlockArgumentOne(value));
                    break;
                case Directive.LOG_APPENDER:
                    config.setLogAppender(getNonBlockArgumentOne(value));
                    break;
                case Directive.LOG_PATH:
                    config.setLogPath(getNonBlockArgumentOne(value));
                    break;
                case Directive.LOG_FORMAT:
                    config.setLogFormat(getNonBlockArgumentListString(value));
                    break;
                default:
                    throw new ConfigParseException(value.getLineNo(), "unknown directive \"" + directive + "\"");
            }
        }
    }
}
