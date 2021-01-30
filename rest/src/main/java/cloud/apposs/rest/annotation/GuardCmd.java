package cloud.apposs.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 保护资源，其工作原理如下：
 * <pre>
 * 1. 配置文件中熔断相关配置，即type/resource/threadhold，配置示例：
 *     <property name="rules">
 *         <value>
 *             <!-- 规则类型 -->
 *             <property name="type">FLOW</property>
 *             <!-- 资源名称 -->
 *             <property name="resource">flow_qps</property>
 *             <!-- 限流阀值 -->
 *             <property name="threshold">20</property>
 *         </value>
 *         ...
 *     </property>
 * 2. 在对应的@Action注解类方法中添加@GuardCmd("flow_qps")进行指定资源熔断注解
 * 3. 如果需要再做细粒度的资源熔断，例如熔断资源为（resource+aid+ip）
 * 4. 则实现{@link cloud.apposs.rest.IGuardProcess}再进行细粒度的资源熔断，再配置熔断规则中的type为LIMITKEY
 * 5、在实现的GuardProcess类上添加@Component让WEBX/BOOTOR底层框架扫描并添加进框架实现中，则每次请求熔断资源前均会调用此方法获取细粒度资源
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GuardCmd {
    /**
     * 资源
     */
    String value();
}
