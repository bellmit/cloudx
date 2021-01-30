package cloud.apposs.rest;

import cloud.apposs.guard.Guard;
import cloud.apposs.guard.ResourceToken;
import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Order;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.rest.auth.AuthManager;
import cloud.apposs.rest.auth.Authentication;
import cloud.apposs.rest.interceptor.HandlerInterceptor;
import cloud.apposs.rest.interceptor.HandlerInterceptorSupport;
import cloud.apposs.rest.interceptor.auth.HandlerAuthInterceptor;
import cloud.apposs.rest.listener.ApplicationListener;
import cloud.apposs.rest.listener.ApplicationListenerSupport;
import cloud.apposs.rest.listener.HandlerListener;
import cloud.apposs.rest.listener.HandlerListenerSupport;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.rest.plugin.Plugin;
import cloud.apposs.rest.plugin.PluginSupport;
import cloud.apposs.rest.view.NoViewResolverFoundException;
import cloud.apposs.rest.view.ViewResolver;
import cloud.apposs.rest.view.ViewResolverSupport;
import cloud.apposs.util.StrUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MVC组件框架
 */
public final class Restful<R, P> {
    /**
     * 框架全局配置
     */
    private RestConfig config;

    /**
     * IOC容器
     */
    private final BeanFactory beanFactory = new BeanFactory();

    /**
     * 插件服务管理
     */
    private final PluginSupport pluginSupport = new PluginSupport();

    /**
     * 框架监听服务管理
     */
    private final ApplicationListenerSupport applicationListenerSupport = new ApplicationListenerSupport();

    /**
     * Hanlder业务服务监听管理
     */
    private final HandlerListenerSupport<R, P> handlerListenerSupport = new HandlerListenerSupport<R, P>();

    /**
     * 拦截器管理
     */
    private final HandlerInterceptorSupport<R, P> handlerInterceptorSupport = new HandlerInterceptorSupport<R, P>();

    /**
     * 处理器映射
     */
    private final HandlerRouter<R, P> handlerRouter = new HandlerRouter<R, P>();

    /**
     * 视图渲染器
     */
    private final ViewResolverSupport<R, P> viewResolverSupport = new ViewResolverSupport<R, P>();

    /**
     * 方法调用辅助类
     */
    private final HandlerInvocation<R, P> handlerInvocation = new HandlerInvocation<R, P>();

    /**
     * 异常解析器，可根据不同的异常实现不同的错误码提示
     */
    private WebExceptionResolver<R, P> webExceptionResolver;

    /**
     * 服务启动时的初始化
     */
    @SuppressWarnings("unchecked")
    public void initialize(RestConfig config) throws Exception {
        this.config = config;
        // 初始化IOC容器，从WebX框架配置扫描包路径中扫描所有Bean实例
        String basePackages = config.getBasePackage();
        if (StrUtil.isEmpty(basePackages)) {
            throw new IllegalStateException("base package not setting");
        }

        // 初始化ApplicationContext到IOC容器中，方便业务获取
        // 注意要提前添加到IOC容器，方便IOC容器load组件时直接获取并调用构造方法
        ApplicationContext context = new ApplicationContext(config, beanFactory);
        beanFactory.addBean(context);

        // 判断是否是以cloud.apposs.xxx, com.example.*作为多包扫描
        String[] basePackageSplit = basePackages.split(",");
        String[] basePackageList = new String[basePackageSplit.length];
        for (int i = 0; i < basePackageSplit.length; i++) {
            basePackageList[i] = basePackageSplit[i].trim();
        }
        // 扫描包将各个IOC组件添加进容器中
        beanFactory.load(basePackageList);

        // 初始化参数解析器，包括用户自定义的和系统定义的，
        // 只要有配置basePackage和Component注解均扫描进来
        List<ParameterResolver> parameterResolverList = beanFactory.getBeanHierarchyList(ParameterResolver.class);
        doSortByOrderAnnotation(parameterResolverList);
        for (ParameterResolver parameterResolver : parameterResolverList) {
            addParameterResolver(parameterResolver);
        }

        // 初始化视图渲染器，包括用户自定义的和系统定义的，同上
        List<ViewResolver> viewResolverList = beanFactory.getBeanHierarchyList(ViewResolver.class);
        doSortByOrderAnnotation(viewResolverList);
        for (ViewResolver viewResolver : viewResolverList) {
            addViewResolver(viewResolver.build(config));
        }

        // 初始化插件服务
        List<Plugin> pluginList = beanFactory.getBeanHierarchyList(Plugin.class);
        for (Plugin plugin : pluginList) {
            plugin.initialize(config);
            pluginSupport.addPlugin(plugin);
        }

        // 初始化容器监听服务
        List<ApplicationListener> appListenerList = beanFactory.getBeanHierarchyList(ApplicationListener.class);
        for (ApplicationListener listener : appListenerList) {
            listener.initialize(config);
            applicationListenerSupport.addListener(listener);
        }
        List<HandlerListener> handlerListenerList = beanFactory.getBeanHierarchyList(HandlerListener.class);
        for (HandlerListener listener : handlerListenerList) {
            listener.initialize(config);
            handlerListenerSupport.addListener(listener);
        }

        // 初始化拦截器
        List<HandlerInterceptor> interceptorList = beanFactory.getBeanHierarchyList(HandlerInterceptor.class);
        // 初始化权限认证拦截服务
        if (config.isAuthDriven()) {
            Authentication authentication = beanFactory.getBeanHierarchy(Authentication.class);
            if (authentication != null) {
                AuthManager.setAuthentication(authentication);
                AuthManager.initialize();
                handlerInterceptorSupport.addInterceptor(new HandlerAuthInterceptor());
            }
        }
        // 对拦截器进行排序后添加
        doSortByOrderAnnotation(interceptorList);
        for (HandlerInterceptor interceptor : interceptorList) {
            interceptor.initialize(context);
            handlerInterceptorSupport.addInterceptor(interceptor);
        }

        // 初始化异常处理器
        webExceptionResolver = beanFactory.getBeanHierarchy(WebExceptionResolver.class);

        // 初始化Handler处理器，
        // 只要有配置basePackage和Action、RestAction注解均扫描进来
        List<Class<?>> actionClassList = beanFactory.getClassAnnotationList(Action.class);
        actionClassList.addAll(beanFactory.getClassAnnotationList(RestAction.class));
        for (Class<?> actionClass : actionClassList) {
            // 获取并遍历该Action类中所有的方法
            Method[] actionMethods = actionClass.getDeclaredMethods();
            for (Method actionMethod : actionMethods) {
                // 处理Action方法
                addHandler(actionClass, actionMethod);
            }
        }
    }

    /**
     * 获取MVC框架IOC容器
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * 添加参数解析器，
     * 负责对{@link cloud.apposs.rest.annotation.Action}中配置的参数进行解析
     */
    public void addParameterResolver(ParameterResolver resolver) {
        handlerRouter.addParameterResolver(resolver);
    }

    /**
     * 添加Action注解对象方法和对应参数解析，
     * 便于之后Http请求时做Url -> Handler映射匹配
     */
    public boolean addHandler(Class<?> actionClass) throws RestException {
        Method[] actionMethods = actionClass.getDeclaredMethods();
        for (Method actionMethod : actionMethods) {
            // 处理Action方法
            handlerRouter.addHandler(actionClass, actionMethod);
        }
        return true;
    }

    public boolean addHandler(Class<?> actionClass, Method actionMethod) throws RestException {
        return handlerRouter.addHandler(actionClass, actionMethod);
    }

    public boolean addHandler(String path, Handler handler) throws RestException {
        return handlerRouter.addHandler(path, handler);
    }

    /**
     * 获取请求对应的{@link Handler}处理器
     */
    public Handler getHandler(IHandlerProcess<R, P> handlerProcess, R request, P response) {
        return handlerRouter.getHandler(handlerProcess, request, response);
    }

    /**
     * 添加视图解析器
     */
    public void addViewResolver(ViewResolver<R, P> viewResolver) {
        viewResolverSupport.addResolver(viewResolver);
    }

    /**
     * 移除视图解析器
     */
    public void removeViewResolver(ViewResolver<R, P> viewResolver) {
        viewResolverSupport.removeResolver(viewResolver);
    }

    /**
     * 添加拦截器
     */
    public void addHandlerInterceptor(HandlerInterceptor<R, P> handlerInterceptor) {
        handlerInterceptorSupport.addInterceptor(handlerInterceptor);
    }

    /**
     * 移除拦截器
     */
    public void removeHandlerInterceptor(HandlerInterceptor<R, P> handlerInterceptor) {
        handlerInterceptorSupport.removeInterceptor(handlerInterceptor);
    }

    /**
     * 调用对应的Handler方法进行业务逻辑处理，
     * 如果Handler有注解写指令则进行服务迁移判断，
     * 如果Handler有注解熔断则进行熔断判断逻辑以保护系统负载，
     */
    public Object invokeHandler(Handler handler, Object target,
                IHandlerProcess<R, P> handlerProcess, R request, P response) throws Exception {
        if (handler.isWriteCmd() && config.isReadOnly()) {
            throw new ReadOnlyException(handlerProcess.getRequestPath(request, response));
        }

        if (!handler.isGuard()) {
            return handlerInvocation.invoke(handler, target, request, response);
        }

        // 如果注解了限流熔断则进入限流判断逻辑
        ResourceToken token = null;
        try {
            IGuardProcess<R, P> guardProcess = handlerProcess.getGuardProcess();
            String resouce = handler.getResource();
            Object limitKey = guardProcess == null ? null : guardProcess.getGuardKey(resouce, request, response);
            token = Guard.entry(resouce, limitKey);
            return handlerInvocation.invoke(handler, target, request, response);
        } catch (Throwable t) {
            // 业务异常埋点以做熔断
            Guard.trace(token, t);
            throw t;
        } finally {
            if (token != null) {
                token.exit();
            }
        }
    }

    public void renderView(Handler handler, Object result, R request, P response) throws Exception {
        renderView(handler, result, request, response, false);
    }

    /**
     * 根据业务逻辑请求结果渲染视图
     *
     * @param flush 是否为异步数据响应
     */
    public void renderView(Handler handler, Object result, R request, P response, boolean flush) throws Exception {
        ViewResolver<R, P> viewResolver = viewResolverSupport.getViewResolver(request, response, result);
        if (viewResolver == null) {
            throw new NoViewResolverFoundException();
        }
        viewResolver.render(request, response, result, flush);
    }

    /**
     * 根据业务逻辑请求结果渲染视图
     */
    public void renderView(IHandlerProcess<R, P> handlerProcess, R request, P response) {
        Handler handler = null;
        Object result = null;
        try {
            handler = getHandler(handlerProcess, request, response);
            if (handler == null) {
                String requestMethod = handlerProcess.getRequestMethod(request, response);
                String requestPath = handlerProcess.getRequestPath(request, response);
                throw new NoHandlerFoundException(requestMethod, requestPath);
            }

            // 请求拦截器调用
            // 如果拦截器中返回false则表示该请求不通过拦截器验证，直接退出
            handlerListenerSupport.handlerStart(request, response, handler);
            if (!handlerInterceptorSupport.preAction(request, response, handler)) {
                return;
            }

            // 如果是异步Handler方法，则先在对应底层内准备好异步条件
            if (handler.isAsync()) {
                handlerProcess.markAsync(request, response);
            }

            // 调用对应的Handler方法进行业务逻辑处理
            // 当前Handler获取的为@Action类，
            // 若为了避免每个请求共用一个@Action类，可以在@Action类中再添加@Prototype
            Object target = beanFactory.getBean(handler.getClazz());
            result = invokeHandler(handler, target, handlerProcess, request, response);

            // 请求处理结束后的拦截器处理
            handlerInterceptorSupport.postAction(request, response, handler);

            // 获取视图解析器进行输出渲染
            if (result instanceof RxIo<?>) {
                // 如果是RxIo响应式编程输出，则采用响应式输出
                RxIo<?> rxIo = (RxIo<?>) result;
                rxIo.subscribe(new RxIoSubcriber(handler, request, response)).start();
            } else {
                // 如果内部业务处理采用异步则直接退出不阻塞主线程，让业务逻辑在处理完成之后再响应输出
                // 注意此处和RxIo的区别是此Handler方法中返回值为非RxIo异步框架，只是标明为@Async，
                // 让内部方法自己进行异步处理的视图渲染，包括直接网络异步发送或者线程池异步处理逻辑
                if (!handler.isAsync()) {
                    renderView(handler, result, request, response);
                }

                // 请求响应结束后的拦截器处理和监听
                handlerInterceptorSupport.afterCompletion(request, response, handler, result);
                handlerListenerSupport.handlerComplete(request, response, handler, result);
            }
        } catch (Throwable ex) {
            // 如果是方法调用中有异常，需要获取的是真正的业务异常
            if (ex instanceof InvocationTargetException) {
                ex = ((InvocationTargetException) ex).getTargetException();
            }
            // 如果配置了自定义异常则处理Handler异常，否则直接抛出异常
            if (webExceptionResolver != null) {
                Throwable webException = ex;
                result = webExceptionResolver.resolveHandlerException(request, response, ex);
                // 获取视图解析器进行输出渲染
                try {
                    boolean async = handler != null && handler.isAsync();
                    renderView(handler, result, request, response, async);
                } catch (Exception e) {
                    webException = e;
                }
                handlerInterceptorSupport.afterCompletion(request, response, handler, result, webException);
                handlerListenerSupport.handlerComplete(request, response, handler, result, webException);
                if (webException != ex) {
                    throw new RestException("Request processing failed by '" + ex + "'", ex);
                }
                return;
            }

            handlerInterceptorSupport.afterCompletion(request, response, handler, result, ex);
            handlerListenerSupport.handlerComplete(request, response, handler, result, ex);
            throw new RestException("Request processing failed by '" + ex + "'", ex);
        }
    }

    /**
     * 根据Order注解进行列表的排序
     */
    private <T> void doSortByOrderAnnotation(List<T> compareList) {
        Collections.sort(compareList, new Comparator<T>() {
            @Override
            public int compare(T object1, T object2) {
                Order order1 = object1.getClass().getAnnotation(Order.class);
                Order order2 = object2.getClass().getAnnotation(Order.class);
                int order1Value = order1 == null ? 0 : order1.value();
                int order2Value = order2 == null ? 0 : order2.value();
                return order1Value - order2Value;
            }
        });
    }

    /**
     * 处理基于RxIo的异步数据响应，
     * 异步处理结束后会触发该类的处理，最终实现异步处理结束后的视图渲染、事件监听和异常处理
     */
    private class RxIoSubcriber implements IoSubscriber<Object> {
        private final Handler handler;

        private final R request;

        private final P response;

        private RxIoSubcriber(Handler handler, R request, P response) {
            this.handler = handler;
            this.request = request;
            this.response = response;
        }

        @Override
        public void onNext(Object value) throws Exception {
            handlerInterceptorSupport.afterCompletion(request, response, handler, value);
            handlerListenerSupport.handlerComplete(request, response, handler, value);
            // 注意此处响应数据渲染视图需要放在最后，
            // 因为request在Action::RxIo.subscribeOn多线程下可能有request数据被清空问题，
            // 具体原因为：
            // 1、当调用renderView进行数据输出时此时由Event_Loop_XXX线程中发送，发送完成后可能请求关闭重置
            // 2、此时此方法中的xxxCompletion是在另外一个线程池执行，而此时获取的值则可能是被重置后的数据了
            // 3、多线程异步下的问题会比较复杂，所以requeset上下文数据获取要做好空值校验
            // 4、详见TimeVariable等对request的上下文获取
            renderView(handler, value, request, response, true);
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable cause) {
            if (webExceptionResolver != null) {
                // 统一框架异常处理，用业务实现的异常解析器解析异常结果并响应输出
                Object result = webExceptionResolver.resolveHandlerException(request, response, cause);
                handlerInterceptorSupport.afterCompletion(request, response, handler, result, cause);
                handlerListenerSupport.handlerComplete(request, response, handler, result, cause);
                // 获取视图解析器进行输出渲染
                try {
                    renderView(handler, result, request, response, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 没有默认的异常处理器则直接终端输出异常
                cause.printStackTrace();
            }
        }
    }

    /**
     * 服务退出时的资源销毁和回调
     */
    public void destroy() {
        // 服务销毁
        pluginSupport.destroy();
        applicationListenerSupport.destroy();
        // 拦截器销毁
        handlerInterceptorSupport.destroy();
        // IOC容器实例销毁
        beanFactory.destroy();
    }
}
