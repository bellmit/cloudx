package cloud.apposs.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否为CMD写命令，
 * 写命令一般用于服务迁移时配置只读，保证线上读服务依然可用，做到迁移时服务的可用性
 * 用于方法上则表示该方法是属于写方法，
 * 用于类上则方法Method为PUT/POST/DELETE时表示为写服务，Method为GET/READ时表示为读服务
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WriteCmd {
}
