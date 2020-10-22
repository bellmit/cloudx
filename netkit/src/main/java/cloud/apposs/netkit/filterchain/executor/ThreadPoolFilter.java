package cloud.apposs.netkit.filterchain.executor;

import cloud.apposs.netkit.IoEvent;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程池过滤器，
 * 包含一个线程池来将事件传递给线程处理，
 * 支持按不同指令用不同的线程池和指定线程组来执行，
 * 但注意，线程池本质还是一个折中方案，推荐最优的方案还是用Handler层用RxIo来做网络异步请求和线程池异步处理
 */
public class ThreadPoolFilter extends IoFilterAdaptor {
    private static final String DEFAULT_THREAD_POOL = "DefaultThreadPool";

    private ThreadPoolHandler handler;

    private final Map<String, ThreadPool> poolGroups;

    public ThreadPoolFilter() {
        this(null);
    }

    public ThreadPoolFilter(ThreadPoolHandler handler) {
        this.handler = handler;
        this.poolGroups = new ConcurrentHashMap<String, ThreadPool>();
    }

    @Override
    public void init() {
        List<ThreadPool> groups = handler.createPoolGroups();
        if (groups == null || groups.isEmpty()) {
            groups = new ArrayList<ThreadPool>(1);
            groups.add(createDefaultThreadPool());
        }

        for (int i = 0; i < groups.size(); i++) {
            ThreadPool pool = groups.get(i);
            poolGroups.put(pool.getName(), pool);
        }
    }

    public ThreadPoolHandler getHandler() {
        return handler;
    }

    public void setHandler(ThreadPoolHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelRead(NextFilter nextFilter,
                            IoProcessor processor, Object message) throws Exception {
        // 因为是异步线程执行，需要先注销读事件，避免EventLoop线路在执行读事件异常导致异步线程执行错误
        final IoEvent event = processor.getEvent();
        final SelectionKey key = processor.selectionKey();
        IoEvent.unRegistSelectionKeyEvent(event, key, IoEvent.OP_READ);

        final ThreadPoolType poolType = handler.getThreadPoolType(message);
        if (poolType != null) {
            final ThreadPoolType.TaskGroup taskType = poolType.getTaskType();
            IoTask task = null;
            if (taskType != null) {
                final String group = taskType.getGroup();
                final int limit = taskType.getLimit();
                task = new IoTask(nextFilter, processor, message, group, limit);
            } else {
                task = new IoTask(nextFilter, processor, message);
            }
            ThreadPool pool = poolGroups.get(poolType.getPoolGroup());
            pool.execute(task);
        } else {
            final IoTask task = new IoTask(nextFilter, processor, message);
            final ThreadPool pool = poolGroups.get(DEFAULT_THREAD_POOL);
            pool.execute(task);
        }
    }

    private static ThreadPool createDefaultThreadPool() {
        return new ThreadPool(DEFAULT_THREAD_POOL);
    }

    @Override
    public void destroy() {
        for (ThreadPool pool : poolGroups.values()) {
            pool.shutdown();
        }
    }
}
