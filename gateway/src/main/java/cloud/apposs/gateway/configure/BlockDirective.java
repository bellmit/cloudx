package cloud.apposs.gateway.configure;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.configure.directive.GlobalAccessDirective;
import cloud.apposs.gateway.configure.directive.HttpDirective;
import cloud.apposs.gateway.configure.directive.LogDirective;
import cloud.apposs.gateway.configure.directive.NetworkDirective;
import cloud.apposs.gateway.configure.directive.PreAccessDirective;
import cloud.apposs.gateway.configure.directive.UpstreamDirective;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link cloud.apposs.gateway.configure.Block}块解析为{@link cloud.apposs.gateway.GatewayConfig}对象接口模块
 */
public class BlockDirective {
    public static final Map<String, IBlockDirective> blockDirectives = new HashMap<String, IBlockDirective>();
    static {
        blockDirectives.put(Directive.NETWORK, new NetworkDirective());
        blockDirectives.put(Directive.LOG, new LogDirective());
        blockDirectives.put(Directive.UPSTREAM, new UpstreamDirective());
        blockDirectives.put(Directive.PREACCESS, new PreAccessDirective());
        blockDirectives.put(Directive.GLOBALACCESS, new GlobalAccessDirective());
        blockDirectives.put(Directive.HTTP, new HttpDirective());
    }

    public static void initialize(List<Block> blockList, GatewayConfig config) throws ConfigParseException {
        for (Block block : blockList) {
            String name = block.getKey();
            IBlockDirective blockDirective = blockDirectives.get(name);
            if (blockDirective == null) {
                throw new ConfigParseException(block.getLineNo(), "unknown directive \"" + name + "\"");
            }
            blockDirective.parse(block, config);
        }
    }

    public interface IBlockDirective {
        void parse(Block block, GatewayConfig config) throws ConfigParseException;
    }
}
