package cloud.apposs.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异步请求标识，
 * 如果Handler处理采用网络异步请求或者线程池异步处理逻辑，则Resful框架不执行渲染直接结束请求线程，
 * 让业务逻辑在处理完成之后再响应输出，但注意要有输出，不做任何处理则底层EventLoop会在检测超时时自动关闭
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
}
