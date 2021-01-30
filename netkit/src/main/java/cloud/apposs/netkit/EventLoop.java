package cloud.apposs.netkit;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.listener.IoListenerSupport;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * IO数据轮询处理，主要负责处理nio底层的数据接收和发送，
 * 数据接收完毕之后交由{@link IoProcessor}做业务逻辑处理
 */
public class EventLoop extends Thread {
    public static final int SELECT_TIMEOUT = 1000;
    private static final String NAME = "Event_Loop-";

    private boolean keepAlive = false;

    private volatile boolean shutdown = false;

    private Selector selector;

    private final Queue<IoProcessor> newProcessores = new ConcurrentLinkedQueue<IoProcessor>();

    public EventLoop() throws IOException {
        this(NAME + 0, false);
    }

    public EventLoop(int index) throws IOException {
        this(NAME + index, false);
    }

    public EventLoop(String name, boolean keepAlive) throws IOException {
        this(name, keepAlive, false);
    }

    public EventLoop(String name, boolean keepAlive, boolean daemon) throws IOException {
        setName(name);
        if (daemon) {
            setDaemon(daemon);
        }
        this.selector = Selector.open();
        this.keepAlive = keepAlive;
    }

    public boolean isRunning() {
        return !shutdown;
    }

    /**
     * 将事件处理器添加到事件轮询中，通过回调的机制来通知事件处理器处理相关业务
     */
    public void addProcessor(IoProcessor processor) {
        if (processor == null) {
            throw new NullPointerException();
        }
        newProcessores.add(processor);
    }

    @Override
    public void run() {
        while (!shutdown) {
            int selected = 0;
            try {
                selected = selector.select(SELECT_TIMEOUT);
            } catch (Exception e) {
                Logger.error(e, "processor select error");
                break;
            }

            // 处理新进的连接
            for (IoProcessor processor = newProcessores.poll(); processor != null; processor = newProcessores.poll()) {
                SelectionKey key = null;
                try {
                    // 将事件处理器注册到Selector选择器中
                    key = processor.register(selector);
                    key.attach(processor);
                } catch (Throwable t) {
                    EventChannel channel = processor.getChannel();
                    if (channel != null) {
                        channel.close();
                    }
                    try {
                        if (key != null) {
                            key.cancel();
                            key.attach(null);
                        }
                    } catch (Throwable e) {
                    }

                    processor.getFilterChain().fireExceptionCaught(t);
                    IoListenerSupport listenerSupport = processor.getListenerSupport();
                    if (listenerSupport != null) {
                        listenerSupport.fireChannelError(processor, t);
                    }
                }
            }

            // 处理请求的连接和数据接收、发送
            if (selected > 0) {
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = readyKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    IoProcessor processor = (IoProcessor) key.attachment();
                    if (processor == null) {
                        doClose(key);
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            doAccept(processor);
                        } else if (key.isConnectable()) {
                            doConnect(processor);
                        } else if (key.isReadable()) {
                            doRecv(processor);
                        } else if (key.isWritable()) {
                            doSend(processor);
                        }

                        // 根据IoProcessor请求的事件决定Selector要重新监听哪些事件
                        doEvent(key);
                    } catch (Throwable cause) {
                        processor.getFilterChain().fireExceptionCaught(cause);
                        IoListenerSupport listenerSupport = processor.getListenerSupport();
                        if (listenerSupport != null) {
                            listenerSupport.fireChannelError(processor, cause);
                        }

                        // 判断该IO事件处理器是否应该直接关闭
                        if (isProcessorClosable(processor, cause)) {
                            doClose(key);
                        }
                    }
                }
            }

            // 检查各个请求是否超时，包括连接、读取、发送超时
            if (!keepAlive) {
                doCheck(selector.keys());
            }
        }
    }

    protected void wakeup() {
        selector.wakeup();
    }

    /**
     * 处理从客户端过来的连接
     */
    private void doAccept(IoProcessor processor) throws Exception {
        EventChannel channel = processor.getChannel();
        processor.getFilterChain().fireChannelAccept(channel);
        IoListenerSupport listenerSupport = processor.getListenerSupport();
        if (listenerSupport != null) {
            listenerSupport.fireChannelAccept(processor);
        }
    }

    /**
     * 处理和服务器建立的连接
     */
    private void doConnect(IoProcessor processor) throws Exception {
        if (processor.getChannel().finishConnect()) {
            processor.getFilterChain().fireChannelConnect();
            IoListenerSupport listenerSupport = processor.getListenerSupport();
            if (listenerSupport != null) {
                listenerSupport.fireChannelConnect(processor);
            }
            processor.getEvent().unInterestEvent(IoEvent.OP_CONNECT);
        }
    }

    /**
     * 处理接收数据，TCP/UDP包数据
     */
    private void doRecv(IoProcessor processor) throws Exception {
        int bufferSize = processor.getBufferSize();
        boolean bufferDirect = processor.isBufferDirect();
        ByteBuf buffer = new ByteBuf(bufferSize, bufferDirect);
        int maxBufferSize = processor.getMaxBufferSize();

        // 从网络网卡中不断读取数据直到读取不到数据
        long ret, readBytes = 0;
        final EventChannel channel = processor.getChannel();
        boolean readComplete = false;
        do {
            if (!buffer.hasWritableBytes()) {
                buffer.expand();
            }
            ret = buffer.channelRecv(channel);
            if (ret <= 0) {
                readComplete = ret < 0;
                break;
            }
            readBytes += ret;
            // 尽量读取多网络数据，但也要保证不超过最大读取字节数避免服务OOM
        } while (!buffer.hasWritableBytes() && readBytes < maxBufferSize);

        if (readBytes > 0) {
            // 将接收的数据包经过IoFilter.channelRead进行数据解码
            processor.getFilterChain().fireChannelRead(buffer);
            IoListenerSupport listenerSupport = processor.getListenerSupport();
            if (listenerSupport != null) {
                listenerSupport.fireChannelRead(processor, readBytes);
            }
        }

        // 远程服务已经关闭连接，不再发送数据，
        // 也代表当次的数据读取已经完全结束，触发channelReadComplete事件
        if (readComplete) {
            processor.getFilterChain().fireChannelReadEof(buffer);
        }

        // 远程服务已经主动关闭连接
        if (readBytes <= 0 && ret <= 0) {
            Logger.debug("channel recv -1 error;flow=%d", processor.getFlow());
            processor.getEvent().setEvent(IoEvent.OP_CLOSE);
        }
    }

    /**
     * 处理发送数据
     */
    private void doSend(IoProcessor processor) throws Exception {
        // 有可能是TCP包也有可能是UDP包，此时的SendBuf已经是经过IoFilter.filterWrite进行数据编码后的数据
        WriteRequest writeRequest = processor.getWriteRequest();
        IoBuffer buffer = writeRequest.getCurrentWriteMessage();
        if (buffer == null) {
            buffer = writeRequest.poll();
            writeRequest.setCurrentWriteRequest(buffer);
        }

        // 要发送的数据为空？
        if (buffer == null) {
            throw new IOException("channel send buffer null error;flow=" + processor.getFlow());
        }

        // 关闭的请求，用于发生错误发送错误信息数据之后关闭会话
        if (buffer == WriteRequest.CLOSE_REQUEST) {
            processor.getEvent().setEvent(IoEvent.OP_CLOSE);
            return;
        }

        long ret, sendBytes = 0;
        int maxBufferSize = processor.getMaxBufferSize() * 3 / 2;
        // 持续发送数据直到数据发送完毕或者达到最大发送数值
        final EventChannel channel = processor.getChannel();
        do {
            ret = buffer.channelSend(channel);
            if (ret <= 0) {
                break;
            }
            sendBytes += ret;
            // 尽量发送多网络数据，但也要保证不超过最大发送字节数避免服务OOM
        } while (buffer.hasReadableBytes() && sendBytes < maxBufferSize);

        // 远程服务已经主动关闭连接
        if (sendBytes <= 0 && ret <= 0) {
            Logger.debug("channel send -1 error;flow=%d", processor.getFlow());
            processor.getEvent().setEvent(IoEvent.OP_CLOSE);
            return;
        }

        // 还有数据要发送，退出等待下次轮询发送
        if (buffer.hasReadableBytes()) {
            return;
        }

        // 所有数据发送完毕
        writeRequest.setCurrentWriteRequest(null);
        if (writeRequest.isEmpty()) {
            processor.getFilterChain().fireChannelSend(writeRequest);
            IoListenerSupport listenerSupport = processor.getListenerSupport();
            if (listenerSupport != null) {
                listenerSupport.fireChannelSend(processor, writeRequest.getTotalSendBytes());
            }
        }
    }

    /**
     * 注册感兴趣事件
     */
    private void doEvent(SelectionKey key) {
        IoProcessor processor = (IoProcessor) key.attachment();
        if (processor == null) {
            return;
        }

        IoEvent event = processor.getEvent();
        // 直接关闭会话请求
        if (event.isEventInterest(IoEvent.OP_CLOSE)) {
            doClose(key);
            return;
        }
        // 正在关闭，不做任何操作，等待数据发送完毕后关闭
        if (event.isEventInterest(IoEvent.OP_CLOSING)) {
            return;
        }
        int newEvent = event.getEvent();
        if (newEvent > 0) {
            int oldEvent = key.interestOps();
            if (oldEvent != newEvent) {
                key.interestOps(newEvent);
            }
        }
    }

    /**
     * 检查各个连接是否超时或者需要关闭
     */
    private void doCheck(Set<SelectionKey> keySet) {
        if (keySet == null || keySet.isEmpty()) {
            return;
        }

        for (SelectionKey key : keySet) {
            IoProcessor processor = (IoProcessor) key.attachment();
            if (processor == null) {
                continue;
            }

            try {
                IoEvent event = processor.getEvent();
                // 直接关闭会话请求
                if (event.isEventInterest(IoEvent.OP_CLOSE)) {
                    doClose(key);
                    continue;
                }
                // 正在关闭，不做任何操作，等待数据发送完毕后关闭
                if (event.isEventInterest(IoEvent.OP_CLOSING)) {
                    continue;
                }

                // 检查超时
                int keyInterestOps = key.interestOps();
                if (IoEvent.isSelectionKeyEventInterest(keyInterestOps, SelectionKey.OP_CONNECT)) {
                    int timeout = processor.getConnectTimeout();
                    int passTime = (int) (System.currentTimeMillis() - processor.getActionTime());
                    if (timeout > 0 && passTime >= timeout) {
                        throw new SocketTimeoutException("Connect Timeout In " + timeout + " Ms");
                    }
                } else if (IoEvent.isSelectionKeyEventInterest(keyInterestOps, SelectionKey.OP_READ)) {
                    int timeout = processor.getRecvTimeout();
                    int passTime = (int) (System.currentTimeMillis() - processor.getActionTime());
                    if (timeout > 0 && passTime >= timeout) {
                        throw new SocketTimeoutException("Recv Timeout In " + timeout + " Ms");
                    }
                } else if (IoEvent.isSelectionKeyEventInterest(keyInterestOps, SelectionKey.OP_WRITE)) {
                    int timeout = processor.getSendTimeout();
                    int passTime = (int) (System.currentTimeMillis() - processor.getActionTime());
                    if (timeout > 0 && passTime >= timeout) {
                        throw new SocketTimeoutException("Send Timeout In " + timeout + " Ms");
                    }
                }
            } catch (Throwable t) {
                processor.getFilterChain().fireExceptionCaught(t);
                IoListenerSupport listenerSupport = processor.getListenerSupport();
                if (listenerSupport != null) {
                    processor.getListenerSupport().fireChannelError(processor, t);
                }
                // 如果业务逻辑处理没有处理超时的直接关闭会话
                IoEvent event = processor.getEvent();
                if (!event.isEventInterest(IoEvent.OP_CLOSING) &&
                        !event.isEventInterest(IoEvent.OP_CLOSE)) {
                    doClose(key);
                }
            }
        }
    }

    /**
     * 关闭句柄连接
     */
    private void doClose(SelectionKey key) {
        IoProcessor processor = (IoProcessor) key.attachment();
        if (processor == null) {
            return;
        }

        try {
            processor.getFilterChain().fireChannelClose();
            IoListenerSupport listenerSupport = processor.getListenerSupport();
            if (listenerSupport != null) {
                processor.getListenerSupport().fireChannelClose(processor);
            }
        } catch (Throwable t) {
            if (key != null) {
                key.cancel();
                key.attach(null);
            }
            t.printStackTrace();
        }
    }

    /**
     * 异常发生可能是业务异常可能是网络异常，需要直接关闭而不进行降级逻辑处理的情况有：
     * 1、直接IOException网络层发生异常，此时对端网络可能已经关闭，再发送数据已无意义
     * 2、业务逻辑处理没有处理异常会主动关闭会话
     */
    private boolean isProcessorClosable(IoProcessor processor, Throwable exception) {
        IoEvent event = processor.getEvent();
        return (
                (exception instanceof IOException) ||
                        (!event.isEventInterest(IoEvent.OP_CLOSING) && !event.isEventInterest(IoEvent.OP_CLOSE))
        );
    }

    public synchronized void shutdownNow() {
        shutdown(true);
    }

    public synchronized void shutdown() {
        shutdown(false);
    }

    /**
     * 关闭IO轮询器
     *
     * @param interrupt 是否立即关闭连接，即打断所有正在执行的轮询器
     */
    public synchronized void shutdown(boolean interrupt) {
        if (shutdown) {
            return;
        }

        shutdown = true;
        if (interrupt) {
            interrupt();
        }
    }
}
