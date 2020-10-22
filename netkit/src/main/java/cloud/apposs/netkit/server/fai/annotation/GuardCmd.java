package cloud.apposs.netkit.server.fai.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 保护资源
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardCmd {

    /**
     * cmd
     */
    int value();

    /**
     * 资源
     */
    String resource();

}
