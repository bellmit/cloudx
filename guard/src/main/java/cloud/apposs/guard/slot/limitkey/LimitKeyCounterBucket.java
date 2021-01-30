package cloud.apposs.guard.slot.limitkey;

import cloud.apposs.guard.tool.DataBucket;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限制关键字计数器数据桶
 */
public class LimitKeyCounterBucket implements DataBucket {

    /**
     * 存着每个 limit key 对应的计数器
     * <pre>
     *     {
     *         limit key 1 -> counter1
     *         limit key 2 -> counter2
     *     }
     * </pre>
     */
    private ConcurrentHashMap<Object, AtomicInteger> limitKeyCounters = new ConcurrentHashMap<Object, AtomicInteger>();

    /**
     * 给指定的 limit key 计数
     */
    public void add(Object limitKey, int token) {
        limitKeyCounters.putIfAbsent(limitKey, new AtomicInteger());
        AtomicInteger counter = limitKeyCounters.get(limitKey);
        counter.addAndGet(token);
    }

    /**
     * 获取 limit key 对应的录入次数
     */
    public Integer get(Object limitKey) {
        AtomicInteger counter = limitKeyCounters.get(limitKey);
        return (counter == null ? 0 : counter.get());
    }

    @Override
    public void reset() {
        limitKeyCounters.clear();
    }
}
