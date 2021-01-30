package cloud.apposs.guard.slot.nodeselector;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.node.ResourceNode;
import cloud.apposs.guard.slotchain.AbstractLinkedProcessorSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 创建数据节点的处理槽
 */
public class NodeSelectorSlot extends AbstractLinkedProcessorSlot {
    private Map<String, Node> resourcesNodes = new HashMap<String, Node>();

    private Lock updateLock = new ReentrantLock();

    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken, int token, Object... args) throws BlockException {
        if (node == null) {
            node = getNode(resource);
        }
        resourceToken.setNode(node);
        fireEntry(resource, node, resourceToken, token, args);
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        fireExit(resource, node, resourceToken, token);
    }

    /**
     * 获取数据节点
     */
    private Node getNode(String resource) {
        Node node = resourcesNodes.get(resource);
        if (node == null) {
            updateLock.lock();
            node = resourcesNodes.get(resource);
            if (node == null) {
                Map<String, Node> newResourceNodes = new HashMap<String, Node>(resourcesNodes.size());
                newResourceNodes.putAll(resourcesNodes);
                newResourceNodes.put(resource, node = new ResourceNode(resource));
                resourcesNodes = newResourceNodes;
            }
            updateLock.unlock();
        }
        return node;
    }
}
