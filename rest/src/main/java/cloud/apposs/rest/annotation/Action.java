package cloud.apposs.rest.annotation;

import cloud.apposs.ioc.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Action类定义，所有Action必须提供空构造函数便于Ioc容器初始化，
 * 注意该Action注解的类在整个Web框架调用中默认为单例，
 * 如果要实现每个请求一个实例则需要添加{@link cloud.apposs.ioc.annotation.Prototype}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Action {
}
