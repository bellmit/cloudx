package cloud.apposs.rest.listener;

import cloud.apposs.rest.Handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link HandlerListener}管理器
 */
public final class HandlerListenerSupport<R, P> {
	/**
     * 创建一个拦截器列表（用于存放拦截器实例）
     */
    private static final List<HandlerListener> listenerList = new CopyOnWriteArrayList<HandlerListener>();
    
    public HandlerListenerSupport() {
    }
    
    public void addListener(HandlerListener listener) {
    	listenerList.add(listener);
    }
    
    /**
     * 请求开始时的监听
     */
    @SuppressWarnings("unchecked")
    public void handlerStart(R request, P response, Handler handler) {
    	for (int i = 0; i < listenerList.size(); i++) {
    		HandlerListener listener = listenerList.get(i);
    		listener.handlerStart(request, response, handler);
    	}
    }
    
    public void handlerComplete(R request, P response, Handler handler, Object result) {
    	handlerComplete(request, response, handler, result, null);
    }
    
    /**
     * 请求结束时的监听
     */
    @SuppressWarnings("unchecked")
    public void handlerComplete(R request, P response, Handler handler, Object result, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
    		HandlerListener listener = listenerList.get(i);
    		listener.handlerComplete(request, response, handler, result, t);
    	}
    }
}
