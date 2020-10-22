package cloud.apposs.netkit.server;

import cloud.apposs.netkit.rxio.IoSubscriber;

public abstract class ServerAction<T> implements IoSubscriber<T> {
	private final ServerHandler handler;
	
	private final ServerHandlerContext context;
	
	public ServerAction(ServerHandler handler, ServerHandlerContext context) {
		this.handler = handler;
		this.context = context;
	}
	
	@Override
	public void onNext(T value) throws Exception {
		onHandle(value);
		onCompleted();
	}
	
	@Override
	public void onError(Throwable t) {
		handler.channelError(context, t);
	}
	
	@Override
	public void onCompleted() {
		try {
			onFinish();
			// 数据处理完毕， 如果有数据需要发送则注册发送事件，
			// 因为异步调用之后有可能该会话不在EventLoop管辖内，需要注册触发EventLoop发送
			context.flush();
		} catch (Throwable t) {
			handler.channelError(context, t);
		}
	}
	
	public abstract void onHandle(T value) throws Exception;
	
	public void onFinish() throws Exception {
	}
}
