package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.rxio.RxIo.OnSubscribe;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 响应式休眠，底层采用的是异步线程定时任务，到达时间后再执行以达到异步休眠的效果
 */
public class OnSubscribeSleep<T> implements OnSubscribe<T> {
    private final RxIo<T> source;

    private final ScheduledExecutorService scheduler;

    private final long sleepTime;

    public OnSubscribeSleep(RxIo<T> source, ScheduledExecutorService scheduler, long sleepTime) {
        this.source = source;
        this.scheduler = scheduler;
        this.sleepTime = sleepTime;
    }

    @Override
    public void call(SafeIoSubscriber<? super T> t) throws Exception {
        SleepSubscriber<T> subscriber = new SleepSubscriber<T>(t);
        source.subscribe(subscriber).start();
    }

    class SleepSubscriber<T> implements IoSubscriber<T> {
        private final IoSubscriber<? super T> actual;

        SleepSubscriber(IoSubscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onNext(T value) throws Exception {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        actual.onNext(value);
                        actual.onCompleted();
                    } catch(Throwable t) {
                        onError(t);
                    }
                }
            }, sleepTime, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable cause) {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        actual.onError(cause);
                        actual.onCompleted();
                    } catch(Throwable t) {
                        onError(t);
                    }
                }
            }, sleepTime, TimeUnit.MILLISECONDS);
        }
    }
}
