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

    public IoProcessor getProcessor() {
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

    public final void flush() {
        processor.flush();
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
