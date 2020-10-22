package cloud.apposs.netkit.filterchain.guard;

import cloud.apposs.guard.Guard;
import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;
import cloud.apposs.util.SysUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源保护过滤器
 * 注意Filter的添加顺便，有些需要在某些拦截器之后，例如 {@link cloud.apposs.netkit.filterchain.fai.FaiFilter} 之后
 */
public class GuardFilter extends IoFilterAdaptor {
    /** cmd -> resource */
    private final Map<Object, String> guardCmdResourceMap = new HashMap<Object, String>();

    private final GuardResource guardResource;

    public GuardFilter(GuardResource guardResource) {
        this.guardResource = guardResource;
    }

    @Override
    public void channelRead(IoFilter.NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
        Object resouceKey = guardResource.getKey(processor, message);
        if (isGuardCmd(resouceKey)) {
            ResourceToken token = null;
            BlockException blockException = null;
            try{
                token = Guard.entry(getResource(resouceKey), guardResource.getArgs(processor, message));
                nextFilter.channelRead(processor, message);
            } catch (BlockException cause) {
                blockException = cause;
                throw cause;
            } finally {
                if (blockException != null) {
                    // 业务异常埋点以做熔断
                    Guard.trace(token, blockException);
                }
                if (token != null) {
                    token.exit();
                }
            }
        } else {
            nextFilter.channelRead(processor, message);
        }
    }

    /**
     * 添加需要被保护的 resourceKey 资源
     * @param resource 需要被保护的资源名
     */
    public void addGuardCmd(Object resourceKey, String resource) {
        SysUtil.checkNotNull(resource, "resource");
        guardCmdResourceMap.put(resourceKey, resource);
    }

    /**
     * 获取资源名称
     */
    private String getResource(Object resourceKey) {
        return guardCmdResourceMap.get(resourceKey);
    }

    /**
     * 判断是否是需要保护的 Cmd
     */
    private boolean isGuardCmd(Object resourceKey) {
        return guardCmdResourceMap.get(resourceKey) != null;
    }

    public interface GuardResource {
        /**
         * 生成对应资源名
         */
        String getKey(IoProcessor processor, Object message);

        /**
         * 生成附带参数，可以是以AID+CMD模式进行附带参数限流，通过实现新的Slot和Rule接口来实现新的限流规则
         */
        Object[] getArgs(IoProcessor processor, Object message);
    }
}
