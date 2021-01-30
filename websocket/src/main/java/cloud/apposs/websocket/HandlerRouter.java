package cloud.apposs.websocket;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.filterchain.websocket.WebSocketSession;
import cloud.apposs.websocket.annotation.ServerEndpoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HandlerRouter {
    /**
     * Handler Map，WebSocket请求与OnOpen方法的映射，
     * 数据结构为：
     * path->List<Handler>，利用此数据结构可以实现一个请求路径的多种匹配，实现根据不同的Methos和Host作不同的Handler匹配
     */
    private final Map<String, List<Handler>> handlers = new ConcurrentHashMap<String, List<Handler>>();

    public void addHandler(Class<? extends WSHandler> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz");
        }

        ServerEndpoint serverEndpoint = clazz.getAnnotation(ServerEndpoint.class);
        if (serverEndpoint == null) {
            throw new IllegalArgumentException("ServerEndpoint not found");
        }
        String host = serverEndpoint.host();
        String[] paths = serverEndpoint.value();
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            Handler handler = new Handler(host, path, clazz);
            List<Handler> handlerList = handlers.get(path);
            if (handlerList == null) {
                handlerList = new LinkedList<Handler>();
                handlers.put(path, handlerList);
            }
            if (doCheckHandlerMatched(handlerList, handler)) {
                throw new IllegalStateException("Handler " + handler + " already exists");
            }
            handlerList.add(handler);
            Logger.info("Mapped %s on %s", path, handler);
        }
    }

    /**
     * 获取请求对应的{@link Handler}处理器
     */
    public Handler getHandler(WebSocketSession session) {
        String path = session.getRequestUri();
        String host = session.getRemoteHost();
        // 根据请求域名、方法、路径先进行路径的精确匹配获取对应的Handler
        List<Handler> handlerList = handlers.get(path);
        if (handlerList != null) {
            // 如果只有一个，则返回默认第一个Handler
            if (handlerList.size() == 1) {
                return handlerList.get(0);
            }
            // 请求路径有多个匹配，则代表可能是HOST不同，获取HOST匹配最精确的那个
            return doGetMatchedHandler(handlerList, host);
        }
        return null;
    }

    /**
     * 判断要添加的Handler是否和已存在的Handler是否匹配，包括请求PATH/HOST/METHODS，
     * 避免业务中有注解重复的请求路径，但方法不同，会有业务逻辑冲突
     */
    private boolean doCheckHandlerMatched(List<Handler> handlerList, Handler handler) {
        for (Handler handler1 : handlerList) {
            String path = handler.getPath();
            if (!handler1.getPath().equalsIgnoreCase(path)) {
                return false;
            }
            String host = handler.getHost();
            if (!handler1.getHost().equalsIgnoreCase(host)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * 对路径匹配的Handler进行匹配排序，包括HOST和METHOD匹配排序，排序算法如下：
     * HOST精确匹配+2分，HOST泛匹配+1分
     */
    private Handler doGetMatchedHandler(List<Handler> matchedHandlerList, String requestHost) {
        Handler matchedHandler = null;
        int matchedScore = 0;
        for (Handler handler : matchedHandlerList) {
            String handlerHost = handler.getHost();
            // 进行HOST匹配评分
            int handlerScore = 0;
            if (handlerHost.equalsIgnoreCase(requestHost)) {
                // HOST精确匹配命中，评分+2
                handlerScore += 2;
            } else if (handlerHost.equals("*")) {
                // HOST泛匹配命中，评分+1
                handlerScore += 1;
            }
            // 取得分最高的匹配Handler
            if (matchedScore < handlerScore) {
                matchedScore = handlerScore;
                matchedHandler = handler;
            }
        }
        return matchedHandler;
    }
}
