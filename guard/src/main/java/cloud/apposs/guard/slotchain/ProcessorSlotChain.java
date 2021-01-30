package cloud.apposs.guard.slotchain;

public abstract class ProcessorSlotChain extends AbstractLinkedProcessorSlot {
    public abstract ProcessorSlotChain addFirst(AbstractLinkedProcessorSlot slot);

    public abstract ProcessorSlotChain addLast(AbstractLinkedProcessorSlot slot);
}
