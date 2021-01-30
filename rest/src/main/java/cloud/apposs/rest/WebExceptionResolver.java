package cloud.apposs.rest;

/**
 * Web全局异常解析器，注意实现的异常在整个Web生命周期中是单例且只能配置一个，
 * 服务于当业务异常时可友好输出异常错误码与提示给前端，或者实现降级服务，
 * 注意JVM在遇到同一异常信息时，前几次会输出堆栈信息，后面就会主动优化掉，只反馈异常摘要信息，
 * 所以需要在JVM启动添加参数： -XX:-OmitStackTraceInFastThrow，
 * 具体参考：
 * <pre>
 * https://blog.csdn.net/zzg1229059735/article/details/72567644
 * https://blog.csdn.net/alivetime/article/details/6166252
 * </pre>
 */
public interface WebExceptionResolver<R, P> {
	/**
     * 解析Handler异常，可根据不同的异常实现不同的错误码提示
     *
     * @param  request   请求对象
     * @param  response  响应对象
     * @param  throwable 异常
     * @return 处理视图结果
     */
    Object resolveHandlerException(R request, P response, Throwable throwable);
}
