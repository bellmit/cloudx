package cloud.apposs.guard.slot.statistic;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统计槽回调注册中心
 */
public final class StatisticSlotCallbackRegistry {
    /**
     * <pre>
     * {
     *      name -> callback instance
     * }
     * </pre>
     */
    private static Map<String, StatisticCallback> callbackList = new ConcurrentHashMap<String, StatisticCallback>();

    public static void addCallback(String name, StatisticCallback callback) {
       callbackList.put(name, callback);
    }

    public static Collection<StatisticCallback> getAllCallback() {
        return callbackList.values();
    }
}
