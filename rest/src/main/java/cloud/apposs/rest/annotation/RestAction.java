package cloud.apposs.rest.annotation;

import cloud.apposs.ioc.annotation.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Action}和{@link WriteCmd}的结合
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Action
@WriteCmd
public @interface RestAction {
}
