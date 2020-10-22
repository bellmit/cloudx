package cloud.apposs.netkit.server.fai;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.rxio.IoSubscriber;

public abstract class FaiAction<T> implements IoSubscriber<T> {
	private final FaiHandler handler;
	
	private final FaiSession session;

	private int flow;
	
	public FaiAction(FaiHandler handler, FaiSession session) {
		this.handler = handler;
		this.session = session;
		this.flow = session.getFlow();
	}
	
	@Override
	public void onNext(T value) throws Exception {
		onHandle(value);
		// 数据处理完毕， 如果有数据需要发送则注册发送事件，
		// 因为异步调用之后有可能该会话不在EventLoop管辖内，需要注册触发EventLoop发送
        // 即有数据要发送，但是当前的 EventLoop 没有对该 Processor 有 WRITE 事件监听
		session.flush();
	}

	@Override
	public void onError(Throwable cause) {
		Logger.error(cause, "asyc call error;flow=%d", flow);
		onFailed(cause);
	}
	
	@Override
	public void onCompleted() {
        try {
            onFinish();
            session.flush();
        } catch (Exception e) {
            session.close(handler.fallback(flow, session, e));
        }
	}
	
	public abstract void onHandle(T value) throws Exception;

	public void onFinish() throws Exception{}

	protected void onFailed(Throwable exp) {
        boolean fallback = handler.fallback(flow, session, exp);
        session.close(fallback);
    }
}
