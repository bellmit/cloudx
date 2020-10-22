package cloud.apposs.netkit.rxio;

import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.actor.Actor;
import cloud.apposs.netkit.rxio.actor.ActorLock;
import cloud.apposs.netkit.rxio.io.http.IoHttp;
import cloud.apposs.util.Errno;
import cloud.apposs.util.StandardResult;
import cloud.apposs.util.SysUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 数据响应式编程
 * 采用RXJAVA类似的响应式开发思维，代码不多，但很绕，核心原理介绍如下
 * <pre>
 *     1. {@link RxIo#create(OnSubscribe)} 负责产生数据流供各个 OnSucribeXXX 进行数据过滤处理
 *     2. {@link RxIo#subscribe(IoSubscriber)} 中 {@link IoSubscriber} 由业务实现对数据流实现最终业务处理
 *     3. OnSucribeXXX 核心是负责对 {@link IoSubscriber} 进行重包装，即对数据流进行包装处理
 * </pre>
 */
public class RxIo<T> {
    public interface OnSubscribe<T> extends IoAction<SafeIoSubscriber<? super T>> {
    }

    protected final OnSubscribe<T> onSubscribe;

    /**
     * 监听数据的订阅者
     */
    private SafeIoSubscriber<? super T> subscriber;

    private RxIo(OnSubscribe<T> f) {
        this.onSubscribe = f;
    }

    public static <T> RxIo<T> create(final OnSubscribe<T> f) {
        return new RxIo<T>(f);
    }

    public static <T> RxIo<T> create(final EventLoopGroup g, final IoProcessor p) {
        return create(new OnSubscribeIo<T>(g, p));
    }

    /**
     * 将数组转换为 RxIo 对象并产生数据流
     */
    public static <T> RxIo<T> from(final T... array) {
        return create(new OnSubscribeFromArray<T>(array));
    }

    /**
     * 将迭代器转换为 RxIo 对象并产生数据流
     */
    public static <T> RxIo<T> from(final Iterable<? extends T> iterable) {
        return create(new OnSubscribeFromIterable<T>(iterable));
    }

    /**
     * 合并多个请求并行调用，
     * 调用结果会逐个触发{@link IoSubscriber#onNext(Object)}进行处理，
     * 所有请求结束后，无论成功还是失败，最终调用一次{@link IoSubscriber#onCompleted()}
     */
    public static <T> RxIo<T> merge(final RxIo<? extends T>... sequences) {
        return create(new OperateorMerge<T>(sequences));
    }

    public static <T> RxIo<T> merge(final List<? extends RxIo<? extends T>> sequences) {
        return create(new OperateorMerge<T>(sequences));
    }

    /**
     * 合并多个请求并行调用，
     * 调用结果会合并到List集合中并触发{@link IoSubscriber#onNext(Object)}进行处理
     */
    public static <T> RxIo<List<T>> mergeList(final RxIo<? extends T>... sequences) {
        return create(new OperateorMergeList<T>(sequences));
    }

    public static <T> RxIo<Boolean> flat(final Iterable<? extends RxIo<? extends T>> sequences,
                                   final IoFunction<? super T, Boolean> predicate) {
        return create(new OperateorFlat<T>(sequences, predicate));
    }

    /**
     * 创建异步http请求
     */
    public static RxIo<HttpAnswer> http(final EventLoopGroup g, final String url) throws Exception {
        return http(g, new IoHttp(url));
    }

    public static RxIo<HttpAnswer> http(final EventLoopGroup g, final IoHttp ioHttp) throws Exception {
        return create(new OnSubscribeIo<HttpAnswer>(g, ioHttp));
    }

    /**
     * 创建异步锁
     */
    public static <T> RxIo<T> lock(final ActorLock key, final Actor actor, final IoEmitter<? extends T> func) {
        return create(new OnSubscribeLock<T>(key, actor, new OnSubscribeEmitter<T>(func)));
    }

    public static <T> RxIo<T> lock(final ActorLock key, final Actor actor, final OnSubscribe<T> f) {
        return create(new OnSubscribeLock<T>(key, actor, f));
    }

    /**
     * 创建异步IO数据流
     */
    public static <T> RxIo<T> emitter(final IoEmitter<? extends T> func) {
        return create(new OnSubscribeEmitter<T>(func));
    }

    /**
     * 创建异步请求
     */
    public final <R> RxIo<R> request(final IoFunction<? super T, ? extends RxIo<? extends R>> func) {
        return create(new OnSubscribeRequest<T, R>(this, func));
    }

    /**
     * 数据变换操作，将一个数据类型转换成另外一个数据类型
     */
    public final <R> RxIo<R> map(final IoFunction<? super T, ? extends R> func) {
        return create(new OnSubscribeMap<T, R>(this, func));
    }

    /**
     * 数据过滤操作，只处理过滤通过的数据
     */
    public final RxIo<T> filter(final IoFunction<? super T, Boolean> predicate) {
        return create(new OnSubscribeFilter<T>(this, predicate));
    }

    /**
     * 数据匹配操作，匹配的数据进行正常流程处理，不匹配的则进入异常流程处理
     */
    public final RxIo<T> match(final IoFunction<? super T, Errno> predicate) {
        return create(new OnSubscribeMatch<T>(this, predicate));
    }

    /**
     * 数据匹配操作，匹配的数据进行正常流程处理，不匹配的则进入异常流程处理
     */
    public final RxIo<T> retry(final IoFunction<Throwable, ? extends RxIo<T>> handler) {
        return create(new OnSubscribeRetry<T>(this, handler));
    }

    /**
     * 数据响应码操作，响应码为成功的数据进行正常流程处理，响应码为失败的则进入异常流程处理
     */
    public final <R> RxIo<StandardResult> handle(final IoFunction<? super T, StandardResult> predicate) {
        return create(new OnSubscribeHandle<T>(this, predicate));
    }

    /**
     * 让业务逻辑在单独的线程池中异步执行，该方法只作用调用链其下的方法
     * 注意该方法要放在调用{@link #subscribe(IoSubscriber)}之前
     */
    public final RxIo<T> executOn(final Executor executor) {
        return create(new OperatorExecutOn<T>(onSubscribe, executor));
    }

    /**
     * 让业务逻辑在单独的定时任务里睡眠一段时间，该方法只作用调用链其下的方法
     * 注意该方法要放在调用{@link #subscribe(IoSubscriber)}之前
     *
     * @param scheduler 底层定时任务，之所以暴露此参数就是在业务层保证底层业务只用一个单例，提升性能
     * @param sleepTime 休眠时间，单位为毫秒
     */
    public final RxIo<T> sleep(ScheduledExecutorService scheduler, long sleepTime) {
        return create(new OnSubscribeSleep<T>(this, scheduler, sleepTime));
    }

    /**
     * 对某一个Observable重复产生多次结果
     */
    public final RxIo<T> repeat(int count) {
        return create(new OnSubscribeRepeat<T>(this, count));
    }

    /**
     * 让业务逻辑在单独的线程池中异步执行，该方法作用调用链其上下的方法，
     * 注意该方法要放在调用{@link #subscribe(IoSubscriber)}之前，
     * 并且对OnSubscribeIo无效，因为OnSubscribeIo是有EventLoop异步网络线程去执行的
     */
    public final RxIo<T> subscribeOn(final Executor executor) {
        return create(new OperatorSubscribeOn<T>(this, executor));
    }

    /**
     * 请求异步请求的输出流传递给{@link IoSubscriber}回调处理
     */
    @SuppressWarnings("unchecked")
    public RxIo<T> subscribe(final IoSubscriber<? super T> subscriber) {
        if (!(subscriber instanceof SafeIoSubscriber<?>)) {
            this.subscriber = new SafeIoSubscriber<T>(subscriber);
        } else {
            this.subscriber = (SafeIoSubscriber<? super T>) subscriber;
        }
        return this;
    }

    public RxIo<T> subscribe(final IoAction<? super T> onNext) {
        SysUtil.checkNotNull(onNext, "onNext");

        IoAction<Throwable> onError = IoActionSubscriber.UNSUPPORTED_ACTION;
        IoAction<Void> onComplete = IoActionSubscriber.EMPTY_ACTION;
        return subscribe(new IoActionSubscriber<T>(onNext, onError, onComplete));
    }

    public RxIo<T> subscribe(final IoAction<? super T> onNext, final IoAction<Throwable> onError) {
        SysUtil.checkNotNull(onNext, "onNext");
        SysUtil.checkNotNull(onError, "onError");

        IoAction<Void> onComplete = IoActionSubscriber.EMPTY_ACTION;
        return subscribe(new IoActionSubscriber<T>(onNext, onError, onComplete));
    }

    public RxIo<T> subscribe(final IoAction<? super T> onNext,
                             final IoAction<Throwable> onError, final IoAction<Void> onComplete) {
        SysUtil.checkNotNull(onNext, "onNext");
        SysUtil.checkNotNull(onError, "onError");
        SysUtil.checkNotNull(onComplete, "onComplete");

        return subscribe(new IoActionSubscriber<T>(onNext, onError, onComplete));
    }

    /**
     * 开始启动异步数据响应处理
     */
    public void start() {
        try {
            onSubscribe.call(subscriber);
        } catch (Throwable t) {
            subscriber.onError(t);
        }
    }
}
