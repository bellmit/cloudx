package cloud.apposs.guard;

import cloud.apposs.guard.slot.fuse.rule.FuseRule;
import cloud.apposs.guard.slot.fuse.rule.FuseRuleManager;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFuseAvgRespTime {
    private static final String RESOURCE_KEY = "MethodA";

    private static AtomicInteger pass = new AtomicInteger();
    private static AtomicInteger block = new AtomicInteger();
    private static AtomicInteger total = new AtomicInteger();

    private static volatile boolean stop = false;
    private static final int threadCount = 100;
    private static int seconds = 60 + 40;

    public static void main(String[] args) {
        // 请求响应时间必须小于100毫秒
        FuseRule rule = new FuseRule(100, FuseRule.FuseGrade.AVG_RESPTIME);
        rule.setResource(RESOURCE_KEY);
        rule.setRestoreTimeInMs(2000);
        rule.setWarningLine(5);
        FuseRuleManager.loadRule(rule);

        // 定期输出熔断请求数据
        simulateTick();

        // 模拟业务请求
        simulateTraffic();
    }

    private static void simulateTick() {
        Thread timer = new Thread(new TimerTask());
        timer.setName("sentinel-timer-task");
        timer.start();
    }

    private static void simulateTraffic() {
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(new RunTask());
            t.setName("simulate-traffic-Task");
            t.start();
        }
    }

    static class TimerTask implements Runnable {
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            System.out.println("begin to statistic!!!");
            long oldTotal = 0;
            long oldPass = 0;
            long oldBlock = 0;
            while (!stop) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
                long globalTotal = total.get();
                long oneSecondTotal = globalTotal - oldTotal;
                oldTotal = globalTotal;

                long globalPass = pass.get();
                long oneSecondPass = globalPass - oldPass;
                oldPass = globalPass;

                long globalBlock = block.get();
                long oneSecondBlock = globalBlock - oldBlock;
                oldBlock = globalBlock;

                System.out.println(System.currentTimeMillis() + ", total:" + oneSecondTotal
                        + ", pass:" + oneSecondPass + ", block:" + oneSecondBlock);
                if (seconds-- <= 0) {
                    stop = true;
                }
            }
            long cost = System.currentTimeMillis() - start;
            System.out.println("time cost: " + cost + " ms");
            System.out.println("total:" + total.get() + ", pass:" + pass.get()
                    + ", block:" + block.get());
            System.exit(0);
        }
    }

    static class RunTask implements Runnable {
        @Override
        public void run() {
            while (!stop) {
                ResourceToken token = null;

                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException e) {
                }
                try {
                    token = Guard.entry(RESOURCE_KEY);
                    pass.addAndGet(1);
                    Random random = new Random();
                    TimeUnit.MILLISECONDS.sleep(random.nextInt(400));
                } catch (Exception e) {
                    block.incrementAndGet();
                } finally {
                    total.incrementAndGet();
                    if (token != null) {
                        token.exit();
                    }
                }
            }
        }
    }
}
