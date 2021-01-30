package cloud.apposs.gateway;

import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.util.AntPathMatcher;
import cloud.apposs.util.SysUtil;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HandlerRouter {
    /**
     * Handler Map(HTTP请求与 Action方法的映射，
     * 数据结构为：
     * path->List<Handler>，利用此数据结构可以实现一个请求路径的多种匹配，实现根据不同的Methos和Host作不同的Handler匹配
     */
    private final Map<String, List<IHandler>> handlers = new ConcurrentHashMap<String, List<IHandler>>();

    /**
     * 参数Url匹配器
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 添加Url -> Handler映射匹配，即网关配置中的location /->Handler映射
     */
    public boolean addHandler(String path, IHandler handler) {
        SysUtil.checkNotNull(handler, "handler");
        List<IHandler> handlerList = handlers.get(path);
        if (handlerList == null) {
            handlerList = new LinkedList<IHandler>();
            handlers.put(path, handlerList);
        }
        handlerList.add(handler);
        return true;
    }

    /**
     * 获取请求对应的{@link IHandler}处理器
     */
    public IHandler getHandler(HttpRequest request, HttpResponse response) {
        String requestMethod = request.getMethod();
        String requestPath = WebUtil.getRequestPath(request);
        String requestHost = request.getRemoteHost();

        // 根据请求域名、方法、路径先进行路径的精确匹配获取对应的Handler
        List<IHandler> handlerList = handlers.get(requestPath);
        if (handlerList != null) {
            // 请求路径有多个匹配，则代表可能是METHOD或者HOST不同，获取METHOD+HOST匹配最精确的那个
            return doGetMatchedHandler(handlerList, requestMethod, requestHost);
        }

        // 没有精确匹配路径，则需要遍历进行路径正则匹配
        List<IHandler> matchedHandlerList = new LinkedList<IHandler>();
        for (String path : handlers.keySet()) {
            handlerList = handlers.get(path);
            Iterator<IHandler> handlerIterator = handlerList.iterator();
            while (handlerIterator.hasNext()) {
                IHandler handler = handlerIterator.next();
                boolean isPattern = handler.isPattern();
                if (!isPattern) {
                    // 非正则路径在前面就已经匹配，不可能在这里命中
                    continue;
                }
                String handlerPath = handler.getPath();
                // 路径必须正则参数匹配
                if (!pathMatcher.match(handlerPath, requestPath)) {
                    continue;
                }
                matchedHandlerList.add(handler);
            }
        }
        // 请求路径正则也没匹配到，直接返回空
        if (matchedHandlerList.isEmpty()) {
            return null;
        }
        // 请求路径有正则匹配，获取METHOD+HOST匹配最精确的那个
        IHandler handler = doGetMatchedHandler(matchedHandlerList, requestMethod, requestHost);
        return handler;
    }

    /**
     * 对路径匹配的Handler进行匹配排序，包括HOST匹配排序，排序算法如下：
     * HOST精确匹配+2分，HOST泛匹配+1分
     */
    private IHandler doGetMatchedHandler(List<IHandler> matchedHandlerList, String requestMethod, String requestHost) {
        IHandler matchedHandler = null;
        int matchedScore = 0;
        for (IHandler handler : matchedHandlerList) {
            String handlerHost = handler.getHost();
            // 进行HOST匹配评分
            int handlerScore = 0;
            if (handlerHost.equalsIgnoreCase(requestHost)) {
                // HOST精确匹配命中，评分+2
                handlerScore += 2;
            } else if (handlerHost.equals(GatewayConstants.GATEWAY_CONF_UNIVERSAL_MATCH)) {
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

    public void close() {
        for (List<IHandler> handlerList : handlers.values()) {
            for (IHandler handler : handlerList) {
                handler.close();
            }
        }
    }
}
