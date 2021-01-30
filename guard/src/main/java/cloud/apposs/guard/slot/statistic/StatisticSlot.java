package cloud.apposs.guard.slot.statistic;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slotchain.AbstractLinkedProcessorSlot;

/**
 * 统计数据的处理槽
 */
public class StatisticSlot extends AbstractLinkedProcessorSlot {
    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken,
                      int token, Object... args) throws BlockException {
        try {
            fireEntry(resource, node, resourceToken, token, args);
            node.addPass(token);

            // 回调注册事件
            for (StatisticCallback statisticCallback : StatisticSlotCallbackRegistry.getAllCallback()) {
                statisticCallback.afterPass(resource, node, resourceToken, token, args);
            }
        } catch (BlockException e) {
            node.addBlock(token);

            // 回调注册事件
            for (StatisticCallback statisticCallback : StatisticSlotCallbackRegistry.getAllCallback()) {
                statisticCallback.afterBlocked(e, resource, node, resourceToken, token, args);
            }
            throw e;
        }
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        if (resourceToken.getException() == null) {
            long createTime = resourceToken.getCreateTime();
            long respTime = System.currentTimeMillis() - createTime;
            node.addRespTimeAndSuccCount(respTime, token);
        } else {
            // 被 block 的 resourceToken
        }
        fireExit(resource, node, resourceToken, token);
    }
}
