package cloud.apposs.guard;

import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRule;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRuleManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestLimitKey {
    private static final String KEY = "MethodA";

    private static final int LIMIT_KEY_AIDS = 10;
    private static Map<Integer, AtomicInteger> pass = new HashMap<Integer, AtomicInteger>();
    private static Map<Integer, AtomicInteger> oldPass = new HashMap<Integer, AtomicInteger>();
    private static Map<Integer, AtomicInteger> block = new HashMap<Integer, AtomicInteger>();
    private static Map<Integer, AtomicInteger> oldBlock = new HashMap<Integer, AtomicInteger>();
    private static Map<Integer, AtomicInteger> total = new HashMap<Integer, AtomicInteger>();
    private static Map<Integer, AtomicInteger> oldTotal = new HashMap<Integer, AtomicInteger>();
    static {
        for (int i = 0; i < LIMIT_KEY_AIDS; i++) {
            pass.put(i, new AtomicInteger());
            oldPass.put(i, new AtomicInteger());
            block.put(i, new AtomicInteger());
            oldBlock.put(i, new AtomicInteger());
            total.put(i, new AtomicInteger());
            oldTotal.put(i, new AtomicInteger());
        }
    }

    private static volatile boolean stop = false;
    private static final int threadCount = 32;
    private static int seconds = 60 + 40;

    public static void main(String[] args) {
        LimitKeyRule rule = new LimitKeyRule();
        rule.setThreshold(6);
        rule.setResource(KEY);
        LimitKeyRuleManager.loadRule(rule);

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

            while (!stop) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                }
                for (int i = 0; i < LIMIT_KEY_AIDS; i++) {
                    AtomicInteger aidTotal = total.get(i);
                    AtomicInteger preTotal = oldTotal.get(i);
                    int globalTotal = aidTotal.get();
                    int oneSecondTotal = globalTotal - preTotal.get();
                    oldTotal.get(i).set(globalTotal);

                    AtomicInteger aidPass = pass.get(i);
                    AtomicInteger prePass = oldPass.get(i);
                    int globalPass = aidPass.get();
                    long oneSecondPass = globalPass - prePass.get();
                    oldPass.get(i).set(globalPass);

                    AtomicInteger aidBlock = block.get(i);
                    AtomicInteger preBlock = oldBlock.get(i);
                    int globalBlock = aidBlock.get();
                    int oneSecondBlock = globalBlock - preBlock.get();
                    oldBlock.get(i).set(globalBlock);

                    System.out.println(seconds + " send aid: " + i + " qps is: " + oneSecondTotal);
                    System.out.println(System.currentTimeMillis() + ", aid:" + i + " total:" + oneSecondTotal
                            + ", pass:" + oneSecondPass
                            + ", block:" + oneSecondBlock);
                }
                if (seconds-- <= 0) {
                    stop = true;
                }
            }

            long cost = System.currentTimeMillis() - start;
            System.out.println("time cost: " + cost + " ms");
            System.exit(0);
        }
    }

    static class RunTask implements Runnable {
        @Override
        public void run() {
            while (!stop) {
                ResourceToken token = null;

                Random random1 = new Random();
                int aid = random1.nextInt(10);
                try {
                    token = Guard.entry(KEY, aid);
                    pass.get(aid).addAndGet(1);
                } catch (BlockException e1) {
                    block.get(aid).incrementAndGet();
                } catch (Exception e2) {
                } finally {
                    total.get(aid).incrementAndGet();
                    if (token != null) {
                        token.exit();
                    }
                }

                Random random2 = new Random();
                try {
                    TimeUnit.MILLISECONDS.sleep(random2.nextInt(50));
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
