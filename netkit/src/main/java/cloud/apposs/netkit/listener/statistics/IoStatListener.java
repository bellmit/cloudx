package cloud.apposs.netkit.listener.statistics;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.listener.IoListenerAdapter;
import cloud.apposs.util.DataCollector;
import cloud.apposs.util.DataDistribution;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

public class IoStatListener extends IoListenerAdapter {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final double[] PERCENTS = {75.0, 95.0, 99.0};

    private static final long CHECK_TIME_WAIT = 60 * 1000L;

    private static final String STAT_CONTEXT = "ServerStatContext";

    private DataCollector collector;

    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private double[] percents = PERCENTS;

    private LogThread logThread;

    private RuntimeDataRecorder dataRecorder = new RuntimeDataRecorder();

    public IoStatListener() {
        DataDistribution collector = new DataDistribution(bufferSize, percents);
        collector.start();
        this.collector = collector;
        this.logThread = new LogThread(collector, dataRecorder);
        this.logThread.start();
    }

    @Override
    public void channelAccept(IoProcessor processor) {
        long now = System.currentTimeMillis();
        Context context = new Context();
        context.setBeginTime(now);
        processor.setAttribute(STAT_CONTEXT, context);
    }

    @Override
    public void channelRead(IoProcessor processor, long readBytesLen) {
        this.dataRecorder.addReadBytes(readBytesLen);
    }

    @Override
    public void channelSend(IoProcessor processor, long sendBytesLen) {
        dataRecorder.addWriteBytes(sendBytesLen);
    }

    @Override
    public void channelError(IoProcessor processor, Throwable t) {
        dataRecorder.countResultErr();
    }

    @Override
    public void channelClose(IoProcessor processor) {
        // 收集耗时
        Context context = (Context) processor.getAttribute(STAT_CONTEXT);
        Long beginTime = context.getBeginTime();
        long spendTime = System.currentTimeMillis() - beginTime;
        collector.collect(spendTime);

        // 收集请求数
        dataRecorder.countReqNum();
    }

    public class Context {
        private long beginTime;

        public long getBeginTime() {
            return beginTime;
        }

        public void setBeginTime(long beginTime) {
            this.beginTime = beginTime;
        }
    }

    /**
     * 输出日志守护线程
     */
    private class LogThread extends Thread {
        private boolean running = true;

        private long lastReqCount = 0;

        private long lastResultErrCount = 0;

        private long lastStatTime = 0;

        private long lastReadBytes = 0;

        private long lastWriteBytes = 0;

        private DataCollector collector;

        private RuntimeDataRecorder dataRecorder;

        LogThread(DataCollector collector, RuntimeDataRecorder dataRecorder) {
            this.setDaemon(true);
            this.collector = collector;
            this.dataRecorder = dataRecorder;
        }

        @Override
        public void run() {
            while (running) {
                doLog();
                try {
                    Thread.sleep(CHECK_TIME_WAIT);
                } catch (InterruptedException e) {
                }
            }
        }

        /**
         * req    请求数
         * ok     请求成功率
         * qps    每秒请求数
         * pxx    75线、95线、99线
         * avg    请求时间平均数
         * rb     读取字节数
         * wb     发送字节数
         */
        private void doLog() {
            long currReqCount = dataRecorder.getReqCount();
            long currResultErrCount = dataRecorder.getResultErrCount();
            long currReadBytes = dataRecorder.getReadBytes();
            long currWriteBytes = dataRecorder.getWriteBytes();
            long now = System.currentTimeMillis();
            long spendTime = now - lastStatTime;

            long req = currReqCount - lastReqCount;
            long okc = currReqCount - lastResultErrCount;
            int ok = 0;
            if (req != 0) {
                ok = (int) Math.round(100.0 * okc / req);
            }
            float qps = (float) (req * 1000.0 / spendTime);
            double p75 = collector.getPercentile(75.0);
            double p95 = collector.getPercentile(95.0);
            double p99 = collector.getPercentile(99.0);
            double avg = collector.getMean();
            String rb = translateBytesCount(currReadBytes - lastReadBytes);
            String wb = translateBytesCount(currWriteBytes - lastWriteBytes);

            String format = "svr stat;rb=%s;wb=%s;avg=%.2f(ms);p75=%.2f(ms);p95=%.2f(ms);p99=%.2f(ms);req=%d;ok=%d%%;qps=%.2f";
            Logger.info(format, rb, wb, avg, p75, p95, p99, req, ok, qps);

            lastReqCount = currReqCount;
            lastResultErrCount = currResultErrCount;
            lastStatTime = now;
            lastReadBytes = currReadBytes;
            lastWriteBytes = currWriteBytes;
        }

        private String translateBytesCount(long bytes) {
            double kiloByte = bytes/1024.0;
            if(kiloByte < 1) {
                return bytes + "Byte(s)";
            }

            double megaByte = kiloByte/1024.0;
            if(megaByte < 1) {
                BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
                return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
            }

            double gigaByte = megaByte/1024.0;
            if(gigaByte < 1) {
                BigDecimal result2  = new BigDecimal(Double.toString(megaByte));
                return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
            }

            double teraBytes = gigaByte/1024.0;
            if(teraBytes < 1) {
                BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
                return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
            }
            BigDecimal result4 = new BigDecimal(teraBytes);
            return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
        }

        private void shutdown(){
            this.running = false;
            interrupt();
        }
    }

    /**
     * 运行数据记录器
     */
    private class RuntimeDataRecorder {
        private AtomicLong reqCount = new AtomicLong(0);

        private AtomicLong resultErrCount = new AtomicLong(0);

        private AtomicLong readBytes = new AtomicLong(0);

        private AtomicLong writeBytes = new AtomicLong(0);

        private void countReqNum() {
            reqCount.incrementAndGet();
        }

        private long getReqCount() {
            return reqCount.get();
        }

        private void countResultErr() {
            resultErrCount.incrementAndGet();
        }

        private long getResultErrCount() {
            return resultErrCount.get();
        }

        private void addReadBytes(long readBytes) {
            this.readBytes.addAndGet(readBytes);
        }

        private void addWriteBytes(long writeBytes) {
            this.writeBytes.addAndGet(writeBytes);
        }

        public long getReadBytes() {
            return readBytes.get();
        }

        public long  getWriteBytes() {
            return writeBytes.get();
        }
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void setPercents(double[] percents) {
        this.percents = percents;
    }
}
