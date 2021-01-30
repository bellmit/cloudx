package cloud.apposs.rest.listener.statistics;

import cloud.apposs.rest.Handler;
import cloud.apposs.rest.listener.HandlerListenerAdapter;
import cloud.apposs.util.DataCollector;
import cloud.apposs.util.DataDistribution;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 每个HTTP会话请求时的监听统计
 */
public abstract class HandlerStatListener<R, P> extends HandlerListenerAdapter<R, P> {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final double[] PERCENTS = {75.0, 95.0, 99.0};

    protected final DataCollector collector;

    private final int bufferSize = DEFAULT_BUFFER_SIZE;

    private final double[] percents = PERCENTS;

    protected final RuntimeDataRecorder dataRecorder = new RuntimeDataRecorder();

    public HandlerStatListener() {
        DataDistribution collector = new DataDistribution(bufferSize, percents);
        collector.start();
        this.collector = collector;
    }

    @Override
    public void handlerStart(R request, P response, Handler handler) {
        setStartTime(request, response, handler);
        // 收集请求数
        dataRecorder.countReqNum();
    }

    @Override
    public void handlerComplete(R request, P response, Handler handler, Object result, Throwable t) {
        if (handler != null) {
            long startTime = getStartTime(request, response, handler);
            if (startTime > 0) {
                long spendTime = System.currentTimeMillis() - startTime;
                collector.collect(spendTime);
            }
        }
        if (t == null) {
            dataRecorder.countResultOk();
        }
    }

    public abstract void setStartTime(R request, P response, Handler handler);

    public abstract long getStartTime(R request, P response, Handler handler);

    /**
     * 运行数据记录器
     */
    public class RuntimeDataRecorder {
        private AtomicLong reqCount = new AtomicLong(0);

        private AtomicLong resultOkCount = new AtomicLong(0);

        public void countReqNum() {
            reqCount.incrementAndGet();
        }

        public long getReqCount() {
            return reqCount.get();
        }

        public void countResultOk() {
            resultOkCount.incrementAndGet();
        }

        public long getResultOkCount() {
            return resultOkCount.get();
        }
    }
}
