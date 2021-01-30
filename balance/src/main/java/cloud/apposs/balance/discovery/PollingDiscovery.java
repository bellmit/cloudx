package cloud.apposs.balance.discovery;

import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.IPeerDiscovery;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定期更新节点任务
 */
public class PollingDiscovery implements IPeerDiscovery {
    private static int DEFAULT_INTERVAL = 10 * 1000;

    private final DiscoveryAction discoveryAction;

    private final AtomicBoolean active = new AtomicBoolean(false);

    private int interval = DEFAULT_INTERVAL;

    private boolean daemon = true;

    private Thread task;

    public PollingDiscovery(DiscoveryAction discoveryAction) {
        this.discoveryAction = discoveryAction;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    @Override
    public synchronized void start(final ILoadBalancer balancer) throws Exception {
        if (active.compareAndSet(false, true)) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    while (active.get()) {
                        // 定时轮询从远程服务中获取新的可用服务实例
                        try {
                            discoveryAction.discover(balancer);
                        } catch (Throwable e) {
                            discoveryAction.cause(e);
                        }

                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            };

            task = new Thread(runnable);
            task.setDaemon(daemon);
            task.start();
        }
    }

    @Override
    public void cause(Throwable cause) {
        discoveryAction.cause(cause);
    }

    @Override
    public synchronized void shutdown() {
        if (active.compareAndSet(true, false)) {
            if (task != null) {
                task.interrupt();
            }
        }
    }

    public interface DiscoveryAction {
        /**
         * 定期获取最新节点信息并更新到{@link ILoadBalancer}中
         */
        void discover(ILoadBalancer balancer) throws Exception;

        /**
         * 定期获取最新节点异常发生时的回调
         */
        void cause(Throwable cause);
    }
}
