package cloud.apposs.guard.slotchain;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;

public class DefaultProcessorSlotChain extends ProcessorSlotChain {
    private AbstractLinkedProcessorSlot first = new AbstractLinkedProcessorSlot() {
        @Override
        public void entry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException {
            fireEntry(resource, node, resourceToken, token, args);
        }

        @Override
        public void exit(String resource, Node node, ResourceToken resourceToken, int token)  {
            fireExit(resource, node, resourceToken, token);
        }
    };

    private AbstractLinkedProcessorSlot last = first;

    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException {
        first.entry(resource, node, resourceToken, token, args);
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        first.exit(resource, node, resourceToken, token);
    }

    @Override
    public ProcessorSlotChain addFirst(AbstractLinkedProcessorSlot slot) {
        slot.setNext(first.getNext());
        first.setNext(slot);
        if (last == first) {
            last = slot;
        }
        return this;
    }

    @Override
    public ProcessorSlotChain addLast(AbstractLinkedProcessorSlot slot) {
        last.setNext(slot);
        last = slot;
        return this;
    }
}
