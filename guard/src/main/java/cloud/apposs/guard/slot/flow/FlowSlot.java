package cloud.apposs.guard.slot.flow;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.exception.FlowBlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.flow.rule.FlowRule;
import cloud.apposs.guard.slot.flow.rule.FlowRuleChecker;
import cloud.apposs.guard.slot.flow.rule.FlowRuleManager;
import cloud.apposs.guard.slotchain.AbstractLinkedProcessorSlot;

import java.util.List;

/**
 * 流控处理槽
 */
public class FlowSlot extends AbstractLinkedProcessorSlot {
    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken,
                      int token, Object... args) throws BlockException {
        checkFlow(resource, node, token);
        fireEntry(resource, node, resourceToken, token, args);
    }

    /**
     * 根据规则检查请求是否被限流
     */
    private void checkFlow(String resource, Node node, int token) throws BlockException{
        List<FlowRule> rules = FlowRuleManager.getRules(resource);
        if (rules == null) {
            return ;
        }
        for (FlowRule rule : rules) {
            if (!canPassCheck(rule, node, token)) {
                throw new FlowBlockException(resource);
            }
        }
    }

    private boolean canPassCheck(FlowRule rule, Node node, int token) {
        return FlowRuleChecker.passCheck(rule, node, token);
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        fireExit(resource, node, resourceToken, token);
    }
}
