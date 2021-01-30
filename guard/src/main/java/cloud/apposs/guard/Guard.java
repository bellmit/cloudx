package cloud.apposs.guard;

import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.slotchain.DefaultSlotChainBuilder;
import cloud.apposs.guard.slotchain.ProcessorSlotChain;
import cloud.apposs.guard.slotchain.SlotChainBuilder;

/**
 * 资源保护入口
 */
public final class Guard {
    private static ProcessorSlotChain chain;

    static {
        SlotChainBuilder builder = new DefaultSlotChainBuilder();
        chain = builder.build();
    }

    public static ResourceToken entry(String resource) throws BlockException {
        return entry(resource, (Object) null);
    }

    /**
     * 尝试进入资源
     * @param resource 资源名
     * @param args 附带参数，可以是以AID+CMD模式进行附带参数限流，通过实现新的Slot和Rule接口来实现新的限流规则
     * @return 进入资源获取令牌 {@link ResourceToken}，可以通过{@link ResourceToken#exit()}来归还令牌
     * @throws BlockException 阻断时抛出
     */
    public static ResourceToken entry(String resource, Object... args) throws BlockException {
        ResourceToken resourceToken = new ResourceToken(resource, chain);
        try {
            chain.entry(resource, null, resourceToken, 1, args);
        } catch (BlockException e) {
            resourceToken.setException(e);
            resourceToken.exit();
            throw e;
        } catch (Exception e) {
            resourceToken.setException(e);
        }
        return resourceToken;
    }

    /**
     * 业务异常统计
     */
    public static void trace(ResourceToken token, Throwable t) {
        trace(token, t, 1);
    }

    public static void trace(ResourceToken token, Throwable cause, int count) {
        if(cause instanceof BlockException) {
            return;
        }
        token.getNode().addException(count);
    }
}
