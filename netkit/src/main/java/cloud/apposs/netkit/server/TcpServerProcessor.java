package cloud.apposs.netkit.server;

import cloud.apposs.netkit.AbstractIoProcessor;
import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.EventSocketChannel;
import cloud.apposs.netkit.WriteRequest;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class TcpServerProcessor extends AbstractIoProcessor {
    private ServerHandler handler;

    private ServerConfig config;

    private final EventSocketChannel channel;

    private final ServerHandlerContext context;

    public TcpServerProcessor(IoServer server,
                              ServerHandler handler, SocketChannel channel) {
        this.handler = handler;
        this.channel = new EventSocketChannel(channel);
        this.context = new ServerHandlerContext(this, server);
    }

    @Override
    public int getBufferSize() {
        return config.getBufferSize();
    }

    @Override
    public boolean isBufferDirect() {
        return config.isBufferDirect();
    }

    @Override
    public int getRecvTimeout() {
        return config.getRecvTimeout();
    }

    @Override
    public int getSendTimeout() {
        return config.getSendTimeout();
    }

    @Override
    public SelectionKey doRegister(Selector selector) throws IOException {
        if (handler.getWelcome() != null) {
            write(handler.getWelcome());
            return channel.register(selector, SelectionKey.OP_WRITE);
        }
        return channel.register(selector, SelectionKey.OP_READ);
    }

    @Override
    public EventChannel getChannel() {
        return channel;
    }

    @Override
    public void channelAccept(EventChannel channel) throws Exception {
        handler.channelAccept(context);
    }

    @Override
    public void channelRead(Object msg) throws Exception {
        handler.channelRead(context, msg);
    }

    @Override
    public void channelSend(WriteRequest request) throws Exception {
        handler.channelSend(context, request);
    }

    @Override
    public void channelError(Throwable cause) {
        handler.channelError(context, cause);
    }

    @Override
    public void channelClose() {
        // 调用父级服务注销网络句柄，避免一直持有网络句柄导致死循环调用
        super.channelClose();
        channel.close();
        handler.channelClose(context);
    }

    public void setConfig(ServerConfig config) {
        this.config = config;
    }
}
