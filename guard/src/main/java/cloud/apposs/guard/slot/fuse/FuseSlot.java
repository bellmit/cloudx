package cloud.apposs.guard.slot.fuse;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.exception.FuseBlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.fuse.rule.FuseRule;
import cloud.apposs.guard.slot.fuse.rule.FuseRuleManager;
import cloud.apposs.guard.slotchain.AbstractLinkedProcessorSlot;

import java.util.List;

/**
 * 熔断处理槽
 */
public class FuseSlot extends AbstractLinkedProcessorSlot {
    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException {
        checkFuseRules(resource, node, token);
        fireEntry(resource, node, resourceToken, token, args);
    }

    private void checkFuseRules(String resource, Node node, int token) throws FuseBlockException {
        List<FuseRule> rules = FuseRuleManager.getRules(resource);
        if (rules == null) {
            return ;
        }
        for (FuseRule rule : rules) {
            if (!FuseRuleChecker.passCheck(node, rule, token)) {
                throw new FuseBlockException(resource);
            }
        }
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        fireExit(resource, node, resourceToken, token);
    }
}
