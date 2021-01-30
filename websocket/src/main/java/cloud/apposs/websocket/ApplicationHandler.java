package cloud.apposs.websocket;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.websocket.WebSocketFrame;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.netkit.server.websocket.WebSocketHandler;
import cloud.apposs.util.StrUtil;
import cloud.apposs.websocket.annotation.ServerEndpoint;

import java.util.List;

public class ApplicationHandler extends WebSocketHandler {
    /** IOC容器 */
    private BeanFactory beanFactory = new BeanFactory();

    /**
     * 处理器映射
     */
    private final HandlerRouter handlerRouter = new HandlerRouter();

    @SuppressWarnings("unchecked")
    public void initialize(WSConfig config) {
        // 初始化IOC容器，从WebX框架配置扫描包路径中扫描所有Bean实例
        String basePackages = config.getBasePackage();
        if (StrUtil.isEmpty(basePackages)) {
            throw new IllegalStateException("base package not setting");
        }
        // 判断是否是以cloud.apposs.xxx, com.example.*作为多包扫描
        String[] basePackageSplit = basePackages.split(",");
        String[] basePackageList = new String[basePackageSplit.length];
        for (int i = 0; i < basePackageSplit.length; i++) {
            basePackageList[i] = basePackageSplit[i].trim();
        }
        // 扫描包将各个IOC组件添加进容器中
        beanFactory.load(basePackageList);
        // 初始化Handler处理器，basePackage包下所有的ServerEndpoint注解类均扫描进来
        List<Class<?>> endpointClassList = beanFactory.getClassAnnotationList(ServerEndpoint.class);
        for (Class<?> endpointClass : endpointClassList) {
            if (WSHandler.class.isAssignableFrom(endpointClass)) {
                // 获取并遍历该ServerEndpoint类中所有的方法，建立Url -> Handler::Frame映射匹配
                handlerRouter.addHandler((Class<? extends WSHandler>) endpointClass);
            }
        }
    }

    @Override
    public boolean onOpen(WebSocketSession session) throws Exception {
        // 获取Url请求对应的Handler处理器
        Handler handler = handlerRouter.getHandler(session);
        if (handler == null) {
            Logger.warn("No Mapping Handler Found For WebSocket Request With URI [" + session.getRequestUri() + "]");
            return false;
        }
        return beanFactory.getBean(handler.getClazz()).onOpen(session);
    }

    @Override
    public void onMessage(WebSocketSession session, WebSocketFrame frame) throws Exception {
        Handler handler = handlerRouter.getHandler(session);
        if (handler != null) {
            beanFactory.getBean(handler.getClazz()).onMessage(session, frame);
        }
    }

    @Override
    public void onClose(WebSocketSession session) {
        Handler handler = handlerRouter.getHandler(session);
        if (handler != null) {
            beanFactory.getBean(handler.getClazz()).onClose(session);
        }
    }

    @Override
    public boolean onError(WebSocketSession session, Throwable cause) throws Exception {
        Handler handler = handlerRouter.getHandler(session);
        if (handler != null) {
            return beanFactory.getBean(handler.getClazz()).onError(session, cause);
        }
        return true;
    }
}
