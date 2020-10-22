package cloud.apposs.netkit.server;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoEvent;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilterChain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.List;

public final class ServerHandlerContext {
    private final IoServer server;

    private final IoProcessor processor;

    public ServerHandlerContext(IoProcessor processor, IoServer server) {
        this.processor = processor;
        this.server = server;
    }

    public final EventLoopGroup getLoopGroup() {
        return server.getEventLoopGroup();
    }

    public final IoEvent getEvent() {
        return processor.getEvent();
    }

    public IoProcessor getP() {
        return processor;
    }

    public long getFlow() {
        return processor.getFlow();
    }

    public void setFlow(long flow) {
        processor.setFlow(flow);
    }

    public final IoFilterChain getFilterChain() {
        return processor.getFilterChain();
    }

    public final void write(byte[] buffer) throws IOException {
        processor.write(buffer);
    }

    public final void write(ByteBuffer buffer) throws IOException {
        processor.write(buffer);
    }

    public final void write(String str) throws IOException {
        processor.write(str);
    }

    public final void write(IoBuffer buffer) throws IOException {
        processor.write(buffer);
    }

    public final void write(List<IoBuffer> buffers) throws IOException {
        processor.write(buffers);
    }

    public final void write(IoBuffer... buffers) throws IOException {
        processor.write(buffers);
    }

    public final void setAttribute(String key, Object value) {
        processor.setAttribute(key, value);
    }

    public final Object getAttribute(String key) {
        return processor.getAttribute(key);
    }

    public final Object getAttribute(String key, Object defaultVal) {
        return processor.getAttribute(key, defaultVal);
    }

    /**
     * 立刻触发EventLoop发送操作，
     * 主要服务于RxIo异步，
     * 因为RxIo的异步性，当前Server所在的EventLoop是其中一个线程，但RxIo中的EventLoop又是另外一个线程，
     * 所以不触发write写事件则当前Server的EventLoop线程是不会主动触发发送事件的
     */
    public final void flush() {
        // 如果有数据要发送，注册发送事件
        if (!processor.getWriteRequest().isEmpty()) {
            final IoEvent event = processor.getEvent();
            final SelectionKey key = processor.selectionKey();
            IoEvent.registSelectionKeyEvent(event, key, IoEvent.OP_WRITE);
        }
    }

    public final void close() {
        processor.close(true);
    }

    public final void closeImmediately() {
        processor.close(true);
    }

    public final void close(boolean immediately) {
        processor.close(immediately);
    }
}
