package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.Directive;
import cloud.apposs.util.Parser;

import java.util.List;

/**
 * network参数解析，包括：num_of_group、backlog、tcp_nodelay
 */
public class NetworkDirective extends AbstractDirective {
    @Override
    public void parse(Block block, GatewayConfig config) throws ConfigParseException {
        checkBlockArgumentMustEmpty(block);
        List<Block> values = block.getValues();
        for (Block value : values) {
            String directive = value.getKey();
            switch (directive) {
                case Directive.NETWORK_NUM_OF_GROUP:
                    config.setNumOfGroup(Parser.parseInt(getNonBlockArgumentOne(value)));
                    break;
                case Directive.NETWORK_BACKLOG:
                    config.setBacklog(Parser.parseInt(getNonBlockArgumentOne(value)));
                    break;
                case Directive.NETWORK_TCP_NODELAY:
                    config.setTcpNoDelay(Parser.parseBoolean(getNonBlockArgumentOne(value)));
                    break;
                default:
                    throw new ConfigParseException(value.getLineNo(), "unknown directive \"" + directive + "\"");
            }
        }
    }
}
