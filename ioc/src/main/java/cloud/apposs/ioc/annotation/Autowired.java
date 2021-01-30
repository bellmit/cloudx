package cloud.apposs.ioc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 反向依赖注入，
 * {@link cloud.apposs.ioc.BeanFactory#getBean(Class)} 会判断是否有该注解并从Bean工厂获取对应实现的对象注入}，
 * 其原理在于，对应Autowired的字段或者方法参数会从IOC容器获取对应的Bean对象并注入进去，代码示例
 * <pre>
 *     // 对应的UserBean和IProductBean在扫描包中需要有对应的实现类
 *     // 字段参数注入
 *     @Autowired
 *     private UserBean user;
 *     // 方法参数注入
 *     @Autowired
 *     public void setProduct(IProductBean product) {
 *         this.product = product;
 *     }
 *     // 构造函数参数注入
 *     @Autowired
 *     public ProductAction(BootorConfig config, OkHttp okHttp) {
 *         this.config = config;
 *         this.okHttp = okHttp;
 *     }
 * </pre>
 * 应用场景：
 * 1. 不建议注解在字段上，在IDE层会提示字段未初始化时就使用警告
 * 2. 推荐注入位置放在构造器上，并在构造器上进行各种初始化操作
 * 3. 其次推荐注入在方法上，主要处理构造方法中参数过多，或者有依赖问题
 * REST框架目前注入的有：
 * 1. BootorConfig/WebXConfig
 * 2. OkHttp
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
}
