package cloud.apposs.bootor.sample.listener;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.rest.Handler;
import cloud.apposs.rest.listener.statistics.HandlerStatListener;
import cloud.apposs.util.DataCollector;

@Component
public class LogStatListener extends HandlerStatListener<HttpRequest, HttpResponse> {
    private static final long CHECK_TIME_WAIT = 60 * 1000L;

    private LogThread logThread;

    public LogStatListener() {
        super();
        this.logThread = new LogThread(collector, dataRecorder);
        this.logThread.start();
    }

    @Override
    public void setStartTime(HttpRequest request, HttpResponse response, Handler handler) {
        request.setAttribute("AttrStartTime", System.currentTimeMillis());
    }

    @Override
    public long getStartTime(HttpRequest request, HttpResponse response, Handler handler) {
        long startTime = (long) request.getAttribute("AttrStartTime", -1);
        return startTime;
    }

    /**
     * 输出日志守护线程
     */
    private class LogThread extends Thread {
        private long lastReqCount = 0;

        private long lastResultOkCount = 0;

        private long lastStatTime = System.currentTimeMillis();

        private DataCollector collector;

        private RuntimeDataRecorder dataRecorder;

        LogThread(DataCollector collector, RuntimeDataRecorder dataRecorder) {
            this.setDaemon(true);
            this.collector = collector;
            this.dataRecorder = dataRecorder;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(CHECK_TIME_WAIT);
                } catch (InterruptedException e) {
                }
                doLog();
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
            long currResultOkCount = dataRecorder.getResultOkCount();
            long now = System.currentTimeMillis();
            long spendTime = now - lastStatTime;

            long req = currReqCount - lastReqCount;
            long okc = currResultOkCount - lastResultOkCount;
            int ok = 0;
            if (req != 0) {
                ok = (int) Math.round(100.0 * okc / req);
            }
            float qps = (float) (req * 1000.0 / spendTime);
            double p75 = collector.getPercentile(75.0);
            double p95 = collector.getPercentile(95.0);
            double p99 = collector.getPercentile(99.0);
            double avg = collector.getMean();

            String format = "svr stat;avg=%.2f(ms);p75=%.2f(ms);p95=%.2f(ms);p99=%.2f(ms);req=%d;ok=%d%%;qps=%.2f";
            Logger.info(format, avg, p75, p95, p99, req, ok, qps);

            lastReqCount = currReqCount;
            lastResultOkCount = currResultOkCount;
            lastStatTime = now;
        }
    }
}
