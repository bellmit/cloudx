package cloud.apposs.netkit;

import cloud.apposs.util.StrUtil;

import java.io.IOException;

/**
 * IO数据轮询组，管理所有的{@link EventLoop}，
 * 目标在于将所有网络请求注册到该轮询器中，实现所有网络请求都用异步
 */
public final class EventLoopGroup {
    public static final int DEFAULT_EVENTLOOP_SIZE =
            Runtime.getRuntime().availableProcessors() + 1;
    public static final String DEFAULT_LOOP_NAME = "Event_Loop-";

    private String loopName = DEFAULT_LOOP_NAME;

    private volatile boolean shutdown = false;

    private boolean keepAlive = false;

    private static long idGenerator = 0L;

    private final EventLoop[] eventLoopPool;

    public EventLoopGroup() {
        this(DEFAULT_EVENTLOOP_SIZE, DEFAULT_LOOP_NAME, false);
    }

    public EventLoopGroup(int size) {
        this(size, DEFAULT_LOOP_NAME, false);
    }

    public EventLoopGroup(int size, boolean keepAlive) {
        this(size, DEFAULT_LOOP_NAME, keepAlive);
    }

    public EventLoopGroup(int size, String name, boolean keepAlive) {
        if (size <= 0) {
            throw new IllegalArgumentException("size");
        }
        if (StrUtil.isEmpty(name)) {
            throw new IllegalArgumentException("name");
        }
        this.loopName = name;
        this.keepAlive = keepAlive;
        // 多个IO轮询器处理数据接收和发送
        this.eventLoopPool = new EventLoop[size];
    }

    public boolean isRunning() {
        return !shutdown;
    }

    public synchronized EventLoopGroup start() throws IOException {
        return start(false);
    }

    public synchronized EventLoopGroup start(boolean daemon) throws IOException {
        if (shutdown) {
            return this;
        }
        for (int i = 0; i < eventLoopPool.length; i++) {
            EventLoop loop = new EventLoop(loopName + i, keepAlive, daemon);
            eventLoopPool[i] = loop;
            loop.start();
        }
        return this;
    }

    /**
     * 将事件处理器添加到事件轮询中，通过回调的机制来通知事件处理器处理相关业务
     */
    public final void addToLoop(IoProcessor processor) {
        EventLoop loop = getLoop();
        loop.addProcessor(processor);
        loop.wakeup();
    }

    public final EventLoop getLoop() {
        if (idGenerator >= Long.MAX_VALUE) {
            idGenerator = 0;
        }
        long id = ++idGenerator;
        EventLoop loop = eventLoopPool[(int) (id % eventLoopPool.length)];
        if (loop == null) {
            throw new IllegalStateException("loop group not start() first error");
        }
        return loop;
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
        for (int i = 0; i < eventLoopPool.length; i++) {
            EventLoop loop = eventLoopPool[i];
            loop.shutdown(interrupt);
        }
    }
}
