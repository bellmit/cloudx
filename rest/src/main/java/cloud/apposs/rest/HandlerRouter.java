package cloud.apposs.rest;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.rest.annotation.Async;
import cloud.apposs.rest.annotation.GuardCmd;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.rest.annotation.WriteCmd;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.rest.parameter.ParameterResolverSupport;
import cloud.apposs.rest.parameter.RequestParameterMissingException;
import cloud.apposs.util.AntPathMatcher;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 处理器路由映射，负责管理Action中的各个方法，实现Request请求和方法匹配
 */
public final class HandlerRouter<R, P> {
    /**
     * Handler Map(HTTP请求与 Action方法的映射，
     * 数据结构为：
     * path->List<Handler>，利用此数据结构可以实现一个请求路径的多种匹配，实现根据不同的Methos和Host作不同的Handler匹配
     */
    private final Map<String, List<Handler>> handlers;

    /**
     * 参数解析器
     */
    private final ParameterResolverSupport parameterSupport;

    /**
     * 参数Url匹配器
     */
    private final AntPathMatcher pathMatcher;

    public HandlerRouter() {
        this.handlers = new ConcurrentHashMap<String, List<Handler>>();
        this.parameterSupport = new ParameterResolverSupport();
        this.pathMatcher = new AntPathMatcher();
    }

    /**
     * 添加Action注解对象方法和对应参数解析，
     * 便于之后Http请求时做Url -> Handler映射匹配，只在系统启动时进行初始化调用
     */
    public boolean addHandler(Class<?> actionClass, Method actionMethod) throws RestException {
        SysUtil.checkNotNull(actionClass, "actionClass");
        SysUtil.checkNotNull(actionMethod, "actionMethod");

        Requestor requestor = doGetRequestor(actionMethod);
        if (requestor == null) {
            return false;
        }

        // 获取方法参数类型和参数注解
        Annotation[][] parameterAnnotations = actionMethod.getParameterAnnotations();
        Annotation[] annotations = new Annotation[parameterAnnotations.length];
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] typeAnnotations = parameterAnnotations[i];
            for (int j = 0; j < typeAnnotations.length; j++) {
                Annotation annotation = typeAnnotations[j];
                annotations[i] = annotation;
                break;
            }
        }

        // 建立方法请求映射
        Class<?>[] parameterTypes = actionMethod.getParameterTypes();
        Parameter[] parameters = new Parameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = null;
            // 判断参数是否配置了Variable注解
            Annotation annotation = annotations[i];
            String parameterName = null;
            if (annotation != null) {
                // Variable参数名称不能为空
                if (Variable.class.isAssignableFrom(annotation.annotationType())) {
                    parameterName = ((Variable) annotation).value();
                    if (StrUtil.isEmpty(parameterName)) {
                        throw new IllegalArgumentException("unset variable name [" + parameterName + "]");
                    }
                }

                parameter = new Parameter(actionMethod, parameterType, i + 1, annotation);
            } else {
                parameter = new Parameter(actionMethod, parameterType, i + 1);
            }

            // 获取参数对应的解析器，即参数绑定
            List<ParameterResolver> resolverList = parameterSupport.getParameterResolverList(parameter);
            if (resolverList.isEmpty()) {
                throw new RequestParameterMissingException(parameter);
            }
            // 参数解析器超过1个，会有混淆风险，输出警告日志
            if (resolverList.size() > 1) {
                Logger.warn("parameter resolver[%s] of parameter[%s] over %d limit",
                        resolverList, parameter, resolverList.size());
            }
            parameter.setResolver(resolverList.get(0));
            parameters[i] = parameter;
        }

        // 添加Path->List<Handler>映射
        String[] paths = requestor.getPath();
        String host = requestor.getHost();
        Request.Method[] methods = requestor.getMethods();
        // 判断是否为异步方法，异步则告诉底层不用阻塞等待业务返回结果，业务自己处理完毕自己输出响应数据
        boolean isAsync = actionMethod.isAnnotationPresent(Async.class);
        if (!isAsync) {
            Class<?> returnType = actionMethod.getReturnType();
            // RxIo返回值的方法的也是异步Handler，由对应{Restful.RxIoSubcriber}异步响应数据
            if (returnType.isAssignableFrom(RxIo.class)) {
                isAsync = true;
            }
        }
        boolean isWriteCmd = isWriteCmd(actionClass, actionMethod, requestor);
        boolean isGuard = actionMethod.isAnnotationPresent(GuardCmd.class);
        Handler handler = new Handler(actionClass, actionMethod, parameters);
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            boolean isPattern = pathMatcher.isPattern(path);
            handler.setMethods(methods).setHost(host).setPath(path)
                    .setPattern(isPattern).setAsync(isAsync).setWriteCmd(isWriteCmd);
            if (isGuard) {
                handler.setGuard(isGuard);
                String resource = actionMethod.getAnnotation(GuardCmd.class).value();
                if (StrUtil.isEmpty(resource)) {
                    resource = handler.toString();
                }
                handler.setResource(resource);
            }
            addHandler(path, handler);
        }

        return true;
    }

    /**
     * 添加Url -> Handler映射匹配，
     * 可供外部系统自定义Handler解析并添加到RESTFUL框架中，只在系统启动时进行初始化调用
     */
    public boolean addHandler(String path, Handler handler) throws RestException {
        SysUtil.checkNotNull(handler, "handler");
        List<Handler> handlerList = handlers.get(path);
        if (handlerList == null) {
            handlerList = new LinkedList<Handler>();
            handlers.put(path, handlerList);
        }
        if (doCheckHandlerMatched(handlerList, handler)) {
            throw new RestException("Handler " + handler + " already exists");
        }
        handlerList.add(handler);
        Logger.info("Mapped %s on %s", handler, doOutputMethod(handler.getMethod()));
        return true;
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
            Request.Method[] methods = handler.getMethods();
            Request.Method[] methods1 = handler1.getMethods();
            if (methods.length != methods1.length) {
                return false;
            }

            for (int i = 0; i < methods.length; i++) {
                Request.Method method = methods[i];
                Request.Method method1 = methods1[i];
                if (!(method.toString().equalsIgnoreCase(method1.toString()))) {
                    return false;
                }
            }

            return true;
        }
        return false;
    }

    /**
     * 获取请求对应的{@link Handler}处理器
     */
    public Handler getHandler(IHandlerProcess<R, P> handlerProcess, R request, P response) {
        String requestMethod = handlerProcess.getRequestMethod(request, response);
        String requestPath = handlerProcess.getRequestPath(request, response);
        String requestHost = handlerProcess.getRequestHost(request, response);

        // 根据请求域名、方法、路径先进行路径的精确匹配获取对应的Handler
        List<Handler> handlerList = handlers.get(requestPath);
        if (handlerList != null) {
            // 请求路径有多个匹配，则代表可能是METHOD或者HOST不同，获取METHOD+HOST匹配最精确的那个
            return doGetMatchedHandler(handlerList, requestMethod, requestHost);
        }

        // 没有精确匹配路径，则需要遍历进行路径正则匹配
        List<Handler> matchedHandlerList = new LinkedList<Handler>();
        for (String path : handlers.keySet()) {
            handlerList = handlers.get(path);
            Iterator<Handler> handlerIterator = handlerList.iterator();
            while (handlerIterator.hasNext()) {
                Handler handler = handlerIterator.next();
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
        Handler handler = doGetMatchedHandler(matchedHandlerList, requestMethod, requestHost);
        // 正则匹配路径参数并保存对应Url参数，如/product/{id}的正则会将/product/12请求中的12参数保存
        handlerProcess.processVariable(request, response,
                pathMatcher.getTemplateVariables(handler.getPath(), requestPath));
        return handler;
    }

    public void addParameterResolver(ParameterResolver resolver) {
        SysUtil.checkNotNull(resolver, "resolver");
        parameterSupport.addParameterResolver(resolver);
    }

    public int getHandlerSize() {
        Collection<List<Handler>> collection = handlers.values();
        int total = 0;
        for (List<Handler> handlerList : collection) {
            total += handlerList.size();
        }
        return total;
    }

    public int getParameterResolverSize() {
        return parameterSupport.getParameterResolverSize();
    }

    /**
     * 输出Hadner Method信息，用于终端/日志输出查看框架加载了哪些Hadnler
     */
    private String doOutputMethod(Method method) {
        StringBuilder builder = new StringBuilder();
        String methodName = method.getName();
        builder.append(Modifier.toString(method.getModifiers())).append(" ");
        builder.append(method.getReturnType().getSimpleName()).append(" ");
        builder.append(methodName);
        builder.append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            builder.append(parameterType.getSimpleName());
            if (i < parameterTypes.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private Requestor doGetRequestor(Method actionMethod) {
        if (actionMethod.isAnnotationPresent(Request.class)) {
            Request annotation = actionMethod.getAnnotation(Request.class);
            String host = annotation.host();
            String[] path = annotation.value();
            Request.Method[] methods = annotation.method();
            return new Requestor(host, path, annotation, methods);
        } else if (actionMethod.isAnnotationPresent(Request.Get.class)) {
            Request.Get annotation = actionMethod.getAnnotation(Request.Get.class);
            String host = annotation.host();
            String[] path = annotation.value();
            return new Requestor(host, path, annotation, Request.Method.GET);
        } else if (actionMethod.isAnnotationPresent(Request.Read.class)) {
            Request.Read annotation = actionMethod.getAnnotation(Request.Read.class);
            String host = annotation.host();
            String[] path = annotation.value();
            Request.Method[] methods = new Request.Method[2];
            methods[0] = Request.Method.GET;
            methods[1] = Request.Method.POST;
            return new Requestor(host, path, annotation, methods);
        } else if (actionMethod.isAnnotationPresent(Request.Post.class)) {
            Request.Post annotation = actionMethod.getAnnotation(Request.Post.class);
            String host = annotation.host();
            String[] path = annotation.value();
            return new Requestor(host, path, annotation, Request.Method.POST);
        } else if (actionMethod.isAnnotationPresent(Request.Put.class)) {
            Request.Put annotation = actionMethod.getAnnotation(Request.Put.class);
            String host = annotation.host();
            String[] path = annotation.value();
            return new Requestor(host, path, annotation, Request.Method.PUT);
        } else if (actionMethod.isAnnotationPresent(Request.Delete.class)) {
            Request.Delete annotation = actionMethod.getAnnotation(Request.Delete.class);
            String host = annotation.host();
            String[] path = annotation.value();
            return new Requestor(host, path, annotation, Request.Method.DELETE);
        } else if (actionMethod.isAnnotationPresent(Request.Head.class)) {
            Request.Head annotation = actionMethod.getAnnotation(Request.Head.class);
            String host = annotation.host();
            String[] path = annotation.value();
            return new Requestor(host, path, annotation, Request.Method.HEAD);
        }
        return null;
    }

    /**
     * 判断指定方法是否有添加@WriteCmd注解，一般在类注解或者方法注解即可开启
     */
    private boolean isWriteCmd(Class<?> actionClass, Method actionMethod, Requestor requestor) {
        boolean isWriteCmdAnnotation = actionMethod.isAnnotationPresent(WriteCmd.class);
        if (isWriteCmdAnnotation) {
            return true;
        }
        isWriteCmdAnnotation = actionClass.isAnnotationPresent(WriteCmd.class)
                || actionClass.isAnnotationPresent(RestAction.class);
        if (isWriteCmdAnnotation) {
            Annotation requestAnnotation = requestor.getAnnotation();
            if (ReflectUtil.isAnnotationEquals(requestAnnotation, Request.Post.class)
                    || ReflectUtil.isAnnotationEquals(requestAnnotation, Request.Delete.class)
                    || ReflectUtil.isAnnotationEquals(requestAnnotation, Request.Put.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对路径匹配的Handler进行匹配排序，包括HOST和METHOD匹配排序，排序算法如下：
     * HOST精确匹配+2分，METHOD精确匹配+2分
     * HOST泛匹配+1分，METHOD泛匹配+1分
     */
    private Handler doGetMatchedHandler(List<Handler> matchedHandlerList, String requestMethod, String requestHost) {
        Handler matchedHandler = null;
        int matchedScore = 0;
        for (Handler handler : matchedHandlerList) {
            String handlerHost = handler.getHost();
            Request.Method[] methods = handler.getMethods();
            // 进行HOST匹配评分
            int handlerScore = 0;
            if (handlerHost.equalsIgnoreCase(requestHost)) {
                // HOST精确匹配命中，评分+2
                handlerScore += 2;
            } else if (handlerHost.equals("*")) {
                // HOST泛匹配命中，评分+1
                handlerScore += 1;
            }
            // 进行METHOD匹配评分
            if (methods.length == 0) {
                // METHOD泛匹配命中，评分+1
                handlerScore += 1;
            } else {
                boolean methodMatched = false;
                for (int i = 0; i < methods.length; i++) {
                    Request.Method method = methods[i];
                    if (method.toString().equalsIgnoreCase(requestMethod)) {
                        // METHOD精确匹配命中，评分+2
                        handlerScore += 2;
                        methodMatched = true;
                        break;
                    }
                }
                // METHOD没有精确匹配命中，评分-2，视为不命中
                if (!methodMatched) {
                    handlerScore -= 2;
                }
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
