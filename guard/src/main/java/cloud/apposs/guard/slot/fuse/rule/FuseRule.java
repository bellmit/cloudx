package cloud.apposs.guard.slot.fuse.rule;

import cloud.apposs.guard.GuardConstants;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.Rule;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断规则
 */
public class FuseRule extends Rule {
    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(), new ResetThreadFactory());

    /**
     * 警告线，不同的熔断策略有不同的意义，
     * {@link FuseGrade#AVG_RESPTIME} 表示时间超过警告线几次开始熔断状态
     * {@link FuseGrade#EXCEPTION_RATE} 表示总请求数超过警戒线才判断是否熔断
     */
    private int warningLine = GuardConstants.DEFAULT_WARNING_LINE;

    /**
     * 熔断恢复时间（ms）
     */
    private long restoreTimeInMs = GuardConstants.DEFAULT_RESTORE_TIME;

    /**
     * 失败阈值
     */
    private double threshold;

    /**
     * 熔断依据
     */
    private FuseGrade grade;

    /**
     * 是否正处于熔断状态
     */
    private AtomicBoolean fusing = new AtomicBoolean(false);

    /**
     * 熔断延迟次数统计
     */
    private AtomicInteger delayCount = new AtomicInteger(0);

    public FuseRule(double threshold, FuseGrade grade) {
        this.threshold = threshold;
        this.grade = grade;
    }

    /**
     * 是否为熔断状态
     */
    public boolean passCheck(Node node) {
        // 异常状态直接拒绝
        if (fusing.get()) {
            return false;
        }

        if (grade == FuseGrade.AVG_RESPTIME) {
            double respTime = node.avgRespTime();
            if (respTime < this.threshold) {
                delayCount.set(0);
                return true;
            }

            // 超过平均时间次数未超过警戒线
            if (delayCount.incrementAndGet() < warningLine) {
                return true;
            }
        } else if (grade == FuseGrade.EXCEPTION_RATE) {
            double exception = node.exceptionQps();
            double success = node.successQps();

            // 没有正常请求数(只有 block 请求) 或者 总请求数未超过警戒线时
            // 直接通过，避免通过数过小导致通过的请求数量少
            if (success < warningLine) {
                return true;
            }

            // 判断异常比例
            if (exception / success < threshold) {
                return true;
            }
        } else if (grade == FuseGrade.EXCEPTION_COUNT) {
            double exception = node.exceptionQps();
            // 判断在统计窗口内是否超过异常数值
            if (exception < threshold) {
                return true;
            }
        }

        // 熔断开启异常状态和自动恢复线程
        if (fusing.compareAndSet(false, true)) {
            ResetTask resetTask = new ResetTask(this);
            pool.schedule(resetTask, restoreTimeInMs, TimeUnit.MILLISECONDS);
        }

        return false;
    }

    public int getWarningLine() {
        return warningLine;
    }

    public void setWarningLine(int warningLine) {
        this.warningLine = warningLine;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public FuseGrade getGrade() {
        return grade;
    }

    public void setGrade(FuseGrade grade) {
        this.grade = grade;
    }

    public long getRestoreTimeInMs() {
        return restoreTimeInMs;
    }

    public void setRestoreTimeInMs(long restoreTimeInMs) {
        this.restoreTimeInMs = restoreTimeInMs;
    }

    @Override
    public String toString() {
        return "FuseRule{" +
                "warningLine=" + warningLine +
                ", restoreTimeInMs=" + restoreTimeInMs +
                ", threshold=" + threshold +
                ", grade=" + grade +
                ", fusing=" + fusing +
                ", delayCount=" + delayCount +
                '}';
    }

    /**
     * 熔断依据枚举类
     */
    public enum FuseGrade {
        /**
         * 根据平均返回时间，如果大于阈值则阻断
         */
        AVG_RESPTIME,
        /**
         * 根据异常比例，如果大于阈值则阻断
         */
        EXCEPTION_RATE,
        /**
         * 根据异常数量，如果大于阈值则阻断
         */
        EXCEPTION_COUNT
    }

    private class ResetTask  implements Runnable{
        private FuseRule rule;

        public ResetTask(FuseRule rule) {
            this.rule = rule;
        }

        @Override
        public void run() {
            this.rule.delayCount.set(0);
            this.rule.fusing.compareAndSet(true, false);
        }
    }

    private static class ResetThreadFactory implements ThreadFactory {
        private AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "reset-fuse-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
