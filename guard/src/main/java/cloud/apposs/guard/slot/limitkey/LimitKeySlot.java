package cloud.apposs.guard.slot.limitkey;

import cloud.apposs.guard.ResourceToken;
import cloud.apposs.guard.exception.BlockException;
import cloud.apposs.guard.exception.LimitKeyException;
import cloud.apposs.guard.node.Node;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRule;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRuleChecker;
import cloud.apposs.guard.slot.limitkey.rule.LimitKeyRuleManager;
import cloud.apposs.guard.slot.statistic.StatisticSlotCallbackRegistry;
import cloud.apposs.guard.slotchain.AbstractLinkedProcessorSlot;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 关键字限流
 */
public class LimitKeySlot extends AbstractLinkedProcessorSlot {
    private static ConcurrentHashMap<String, LimitKeyMetric> metricMap = new ConcurrentHashMap<String, LimitKeyMetric>();

    private static final Lock UPDATE_LOCK = new ReentrantLock();

    public LimitKeySlot() {
        StatisticSlotCallbackRegistry.addCallback(LimitKeyStatisticCallback.class.getName(), new LimitKeyStatisticCallback());
    }

    @Override
    public void entry(String resource, Node node, ResourceToken resourceToken,
                      int token, Object... args) throws BlockException {
        checkFlow(resource, node, token, args);
        fireEntry(resource, node, resourceToken, token, args);
    }

    /**
     * 检查限流逻辑
     */
    private void checkFlow(String resource, Node node, int token, Object... args) throws LimitKeyException {
        if (args == null || args.length <= 0 || args[0] == null) {
            return;
        }
        Object limitKey = args[0];
        LimitKeyMetric metric = initAndGetMetric(resource);
        List<LimitKeyRule> rules = LimitKeyRuleManager.getRules(resource);
        for (LimitKeyRule rule : rules) {
            if (!LimitKeyRuleChecker.passCheck(metric, rule, token, limitKey)) {
                throw new LimitKeyException(resource, limitKey);
            }
        }
    }

    /**
     * 初始化一个数据托管
     */
    private LimitKeyMetric initAndGetMetric(String resource) {
        LimitKeyMetric metric;
        if ((metric = metricMap.get(resource)) == null) {
            try{
                UPDATE_LOCK.lock();
                if ((metric = metricMap.get(resource)) == null){
                    metric = metricMap.put(resource, new LimitKeyMetric());
                }
            } finally {
                UPDATE_LOCK.unlock();
            }
        }
        return metric;
    }

    /**
     * 根据资源获取数据托管
     */
    public static LimitKeyMetric getMetric(String resource) {
        return metricMap.get(resource);
    }

    @Override
    public void exit(String resource, Node node, ResourceToken resourceToken, int token) {
        fireExit(resource, node, resourceToken, token);
    }
}
