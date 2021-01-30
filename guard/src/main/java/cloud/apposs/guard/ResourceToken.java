package cloud.apposs.guard;

import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slotchain.ProcessorSlotChain;

/**
 * 资源令牌，只有正常获取令牌才可以进入资源
 */
public class ResourceToken {
    private String resource;

    private ProcessorSlotChain chain;

    private long createTime;

    private Exception exception;

    private Node node;

    public ResourceToken(String resource, ProcessorSlotChain chain) {
        this.resource = resource;
        this.createTime = System.currentTimeMillis();
        this.chain = chain;
    }

    /**
     * 正常进入资源之后通过这个方法归还令牌
     */
    public void exit(){
        chain.exit(resource, node, this, 1);
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public long getCreateTime() {
        return createTime;
    }

    public Node getNode() {
        return node;
    }
}
