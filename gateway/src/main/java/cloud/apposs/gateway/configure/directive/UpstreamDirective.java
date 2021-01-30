package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.GatewayConstants;
import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.Directive;
import cloud.apposs.util.Parser;

import java.util.List;

public class UpstreamDirective extends AbstractDirective {
    @Override
    public void parse(Block block, GatewayConfig config) throws ConfigParseException {
        String upstreamName = getBlockArgumentOne(block);
        List<Block> blockServerList = block.getValues();
        for (Block blockServer : blockServerList) {
            String blockServerKey = blockServer.getKey();
            if (!blockServerKey.equals(Directive.UPSTREAM_SERVER)) {
                throw new ConfigParseException(blockServer.getLineNo(), "unknown directive \"" + blockServerKey + "\"");
            }
            String blockServerValue = getNonBlockArgumentOne(blockServer);
            String[] blockServerSplit = blockServerValue.split(":");
            String serverHost = null;
            int serverPort = GatewayConstants.DEFAULT_PROXY_PORT;
            if (blockServerSplit.length == 1) {
                serverHost = blockServerSplit[0];
            } else if (blockServerSplit.length == 2) {
                serverHost = blockServerSplit[0];
                serverPort = Parser.parseInt(blockServerSplit[1]);
            } else {
                throw new ConfigParseException(blockServer.getLineNo(), "invalid number of arguments in \""+ blockServerKey + "\" directive");
            }
            config.addUpstreamServer(upstreamName, new GatewayConfig.UpstreamServer(serverHost, serverPort));
        }
    }
}
