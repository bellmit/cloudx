package cloud.apposs.gateway.configure.directive;

import cloud.apposs.gateway.GatewayConfig;
import cloud.apposs.gateway.configure.Block;
import cloud.apposs.gateway.configure.ConfigParseException;
import cloud.apposs.gateway.configure.Directive;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;

import java.util.List;

public class PreAccessDirective extends AbstractDirective {
    @Override
    public void parse(Block block, GatewayConfig config) throws ConfigParseException {
        String preacessName = getBlockArgumentOne(block);
        List<Block> interceptorList = block.getValues();
        for (Block interceptorBlock : interceptorList) {
            String interceptorKey = interceptorBlock.getKey();
            if (!interceptorKey.equals(Directive.PREACCESS_INTERCEPTOR)) {
                throw new ConfigParseException(interceptorBlock.getLineNo(), "unknown directive \"" + interceptorKey + "\"");
            }
            String interceptorName = getBlockArgumentOne(interceptorBlock);
            List<Block> interceptorValues = interceptorBlock.getValues();
            String interceptorClassName = null;
            Param interceptorArgs = new Param();
            for (Block interceptorValue : interceptorValues) {
                String interceptorDirective = interceptorValue.getKey();
                switch (interceptorDirective) {
                    case Directive.PREACCESS_NAME:
                        interceptorClassName = getNonBlockArgumentOne(interceptorValue);
                        break;
                    case Directive.PREACCESS_ARG:
                        List<String> interceptorStringArgs = getNonBlockArgumentListTwo(interceptorValue);
                        interceptorArgs.put(interceptorStringArgs.get(0), interceptorStringArgs.get(1));
                        break;
                    default:
                        throw new ConfigParseException(interceptorBlock.getLineNo(), "unknown directive \"" + interceptorDirective + "\"");
                }
            }
            if (StrUtil.isEmpty(interceptorClassName)) {
                throw new ConfigParseException(interceptorBlock.getLineNo(), "invalid number of arguments in \""+ interceptorKey + "\" directive");
            }
            if (interceptorClassName.indexOf(".") == -1) {
                interceptorClassName = "cloud.apposs.gateway.interceptor." +
                        interceptorName + "." + StrUtil.upperCamelCase(interceptorClassName);
            }
            GatewayConfig.Interceptor interceptor = new GatewayConfig.Interceptor(interceptorClassName);
            interceptor.getArguments().putAll(interceptorArgs);
            config.addInterceptor(preacessName, interceptor);
        }
    }
}
