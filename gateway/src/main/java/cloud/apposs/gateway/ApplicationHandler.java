package cloud.apposs.gateway;

import cloud.apposs.gateway.handler.IHandler;
import cloud.apposs.gateway.interceptor.HandlerInterceptor;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.server.http.HttpHandler;
import cloud.apposs.netkit.server.http.HttpSession;
import cloud.apposs.util.AntPathMatcher;
import cloud.apposs.util.HttpStatus;

import java.util.List;

/**
 * netkit底层会话处理器，每个http请求均在此类处理，全局单例，
 * 负责路径匹配，网关过滤（包括限流、鉴权等）、请求转发等
 */
public class ApplicationHandler extends HttpHandler {
    /**
     * 处理器映射路由
     */
    private final HandlerRouter handlerRouter = new HandlerRouter();

    /**
     * 参数Url匹配器
     */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 网关统一异常处理服务
     */
    private final GatewayExceptionResolver exceptionResolver = new GatewayExceptionResolver();

    /**
     * 根据配置初始化此网络请求处理器，
     * 一般在服务启动时初始化，也可用于配置更新时的热加载初始化
     */
    @SuppressWarnings("unchecked")
    public void initialize(GatewayConfig config) throws Exception {
        // 添加请求路径处理器
        List<GatewayConfig.Location> locations = config.getLocations();
        for (GatewayConfig.Location location : locations) {
            Class<IHandler> handlerClass = (Class<IHandler>) Class.forName(location.getClazz());
            IHandler handler = handlerClass.newInstance();
            // 初始化IHandler拦截器实例
            List<GatewayConfig.Interceptor> interceptorList = location.getInterceptorList();
            if (interceptorList != null) {
                for (GatewayConfig.Interceptor interceptor : interceptorList) {
                    String interceptorClassName = interceptor.getName();
                    Class<HandlerInterceptor> handlerInterceptorClass = (Class<HandlerInterceptor>) Class.forName(interceptorClassName);
                    HandlerInterceptor handlerInterceptor = handlerInterceptorClass.newInstance();
                    handlerInterceptor.init(interceptor.getArguments());
                    handler.addInterceptor(handlerInterceptor);
                }
            }
            // 初始化IHandler实例参数，便于数据处理时根据conf配置参数进行对应的逻辑处理
            String path = location.getPath();
            handler.initialize(location.getOptions());
            handler.setHost(location.getHost());
            handler.setContentType(location.getContentType());
            handler.addHeaders(location.addHeaders());
            handler.proxyHeaders(location.proxyHeaders());
            handler.setPath(path);
            handler.setPattern(pathMatcher.isPattern(path));
            // 一个location path对应一个IHandler实例
            handlerRouter.addHandler(path, handler);
        }
    }

    /**
     * 所有请求网关入口
     */
    @Override
    public void service(HttpSession session) throws Exception {
        HttpRequest request = session.getRequest();
        HttpResponse response = session.getResponse();
        try {
            // 根据请求路径获取对应匹配的IHandler处理器
            IHandler handler = handlerRouter.getHandler(request, response);
            if (handler == null) {
                throw new GatewayException(HttpStatus.HTTP_STATUS_404,
                        "No Mapping Handler Found For HTTP Request With URI [" + WebUtil.getRequestPath(request) + "]");
            }

            // 响应处理数据
            // 请求处理结束前的拦截器处理
            if (!handler.getInterceptorSupport().preAction(request, response, handler)) {
                return;
            }
            // 异步处理响应结果
            RxIo<?> rxIo = handler.handle(request, response);
            rxIo.subscribe(new RxIoSubcriber(handler, request, response)).start();
        } catch (Throwable ex) {
            exceptionResolver.resolveException(request, response, ex);
        }
    }

    private class RxIoSubcriber implements IoSubscriber<Object> {
        private final IHandler handler;

        private final HttpRequest request;

        private final HttpResponse response;

        private RxIoSubcriber(IHandler handler, HttpRequest request, HttpResponse response) {
            this.handler = handler;
            this.request = request;
            this.response = response;
        }

        @Override
        public void onNext(Object value) throws Exception {
            String contentType = handler.getContentType();
            String charset = handler.getCharset();
            response.setContentType(contentType + "; charset=" + charset);
            response.write(value.toString(), true);
            handler.getInterceptorSupport().afterCompletion(request, response, handler);
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable cause) {
            try {
                exceptionResolver.resolveException(request, response, cause);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭此会话处理器，释放资源，
     * 一般用于网关服务关闭时或者配置文件热加载时，旧的服务替换并释放资源
     */
    public void release() {
        handlerRouter.close();
    }
}
