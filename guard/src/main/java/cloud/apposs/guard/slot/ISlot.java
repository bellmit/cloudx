package cloud.apposs.guard.slot;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;

/**
 * 处理槽
 */
public interface ISlot {
    /**
     * 执行槽入口
     *
     * @param resource 需要访问的资源
     * @param token 需要的令牌数
     * @param args 附加参数
     * @throws BlockException 被阻断时抛出
     */
    void entry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException;

    /**
     * 执行完 {@link #entry} 后执行此方法
     */
    void fireEntry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException;

    /**
     * 退出槽入口
     */
    void exit(String resource, Node node, ResourceToken resourceToken, int token);

    /**
     * 执行完 {@link #exit} 后执行此方法
     */
    void fireExit(String resource, Node node, ResourceToken resourceToken, int token);
}
