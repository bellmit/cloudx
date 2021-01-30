package cloud.apposs.guard.slotchain;

import cloud.apposs.guard.slot.flow.FlowSlot;
import cloud.apposs.guard.slot.fuse.FuseSlot;
import cloud.apposs.guard.slot.limitkey.LimitKeySlot;
import cloud.apposs.guard.slot.nodeselector.NodeSelectorSlot;
import cloud.apposs.guard.slot.statistic.StatisticSlot;

public class DefaultSlotChainBuilder implements SlotChainBuilder {
    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();
        chain.addLast(new NodeSelectorSlot());
        chain.addLast(new StatisticSlot());
        chain.addLast(new LimitKeySlot());
        chain.addLast(new FlowSlot());
        chain.addLast(new FuseSlot());
        return chain;
    }
}
