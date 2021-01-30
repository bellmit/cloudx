package cloud.apposs.guard.slotchain;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.ISlot;

/**
 * 向下链式处理槽
 */
public abstract class AbstractLinkedProcessorSlot implements ISlot {
    private AbstractLinkedProcessorSlot next = null;

    @Override
    public void fireEntry(String resource, Node node,
                          ResourceToken resourceToken, int token, Object... args) throws BlockException {
        if (next != null) {
            next.entry(resource, node, resourceToken, token, args);
        }
    }

    @Override
    public void fireExit(String resource, Node node, ResourceToken resourceToken, int token) {
        if (next != null) {
            next.exit(resource, node, resourceToken, token);
        }
    }

    public AbstractLinkedProcessorSlot getNext() {
        return next;
    }

    public void setNext(AbstractLinkedProcessorSlot next) {
        this.next = next;
    }
}
