package cloud.apposs.netkit.server.http;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;

import java.net.SocketTimeoutException;

public abstract class HttpHandler extends ServerHandlerAdaptor {
    public static final String CONTEXT_SESSION = "HttpSession";

    @Override
    public void channelRead(ServerHandlerContext context, Object message) throws Exception {
        HttpRequest request = (HttpRequest) message;
        HttpSession session = new HttpSession(context, request);
        context.setAttribute(CONTEXT_SESSION, session);
        try {
            service(session);
        } catch (Exception e) {
            context.close(fallback(session, e));
        }
    }

    @Override
    public void channelError(ServerHandlerContext context, Throwable cause) {
        if (cause instanceof SocketTimeoutException) {
            context.close(true);
            return;
        }
        HttpSession session = (HttpSession) context.getAttribute(CONTEXT_SESSION);
        if (session == null) {
            context.close(true);
            return;
        }

        boolean fallback = true;
        try {
            fallback = fallback(session, cause);
        } catch (Throwable t) {
            Logger.error(t, "HttpHandler fallback error");
        }
        session.close(fallback);
    }

    /**
     * 所有会话异常时的回调，主要服务于降级
     *
     * @return true则直接告诉底层把包扔掉，false则自己处理这些包并发送给客户端，不做任何降级实现默认返回-1错误码给客户端
     */
    public boolean fallback(HttpSession session, Throwable cause) throws Exception {
        Logger.error(cause, "HttpHandler fallback error");
        return true;
    }

    /**
     * 业务逻辑处理，
     * 因为底层netkit也是采用异步的方式，所以该方法采用RxIo或者线程池异步直接返回也不会关闭会话，
     * 那么关闭HTTP请求会话只有以下几种情况：
     * 1、当数据发送完毕时，此时如果HTTP协议为1.0则立刻关闭，否则保持HTTP长连接直到超时或者客户端主动关闭
     * 2、业务处理有异常立即关闭请求会话
     * 3、底层EventLoop检查到IoProcessor已经超时了，关闭请求会话
     */
    public abstract void service(HttpSession session) throws Exception;
}
