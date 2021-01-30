package cloud.apposs.rest.listener;

import java.util.LinkedList;
import java.util.List;

public final class ApplicationListenerSupport {
	/** 监听列表 */
    private static final List<ApplicationListener> listenerList = new LinkedList<ApplicationListener>();
    
    public void addListener(ApplicationListener listener) {
    	if (listener != null) {
    		listenerList.add(listener);
    	}
    }
    
    public void removeListener(ApplicationListener listener) {
    	if (listener != null) {
    		listenerList.remove(listener);
    	}
    }
    
    /**
     * Web容器关闭时的插件销毁
     */
    public void destroy() {
    	for (int i = 0; i < listenerList.size(); i++) {
    		ApplicationListener listener = listenerList.get(i);
    		listener.destroy();
    	}
    }
}
