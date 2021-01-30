package cloud.apposs.rest;

/**
 * 统一异常处理接口
 */
public interface ExceptionHandler<R, P> {
    /**
     * 获取要匹配的异常类型
     */
    Class<? extends Throwable> getExceptionType();

    /**
     * 对匹配的异常进行解析
     */
    Object resloveException(R request, P response, Throwable throwable);
}
