package cloud.apposs.rest;

import cloud.apposs.ioc.BeanFactory;
import cloud.apposs.ioc.BeansException;
import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.annotation.Order;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 应用程序上下文，业务基于此上下文实现扩展功能，例如基于此组件实现策略模式，
 * 示例如下：
 * <pre>
 *     // 1. 先定义接口
 *     interface IXXXHandler {}
 *     // 2. 实现指定接口功能并注解为@Component组件
 *     @Component
 *     class XXXHandler implements IXXXHandler {}
 *     // 3. 在对应核心类中通过ApplicationContext获取对应的IXXXHandler实现类
 *     List<IXXXHandler> handlerList = context.getBeanHierarchyList(IXXXHandler.class);
 *     for (IFileHandler handler : handlerList) {
 *         handlers.put(handler.getApp(), handler);
 *     }
 *     // 4. 业务实现获取指定的实现类进行接口逻辑处理
 *     handlers.get(XXX).XXX
 *     // 5. 此模式为Map策略模式的升级，只需要业务将实现类添加到扫描包目录即可
 * </pre>
 * 参考：https://mp.weixin.qq.com/s/dEqb8c3S2LlgWow6WUg0zA
 */
@Component
public class ApplicationContext {
    private final RestConfig config;

    /**
     * IOC容器
     */
    private final BeanFactory beanFactory;

    public ApplicationContext(RestConfig config, BeanFactory beanFactory) {
        this.config = config;
        this.beanFactory = beanFactory;
    }

    public RestConfig getConfig() {
        return config;
    }

    public <T> T getBean(Class<T> beanClass) throws BeansException {
        return beanFactory.getBean(beanClass);
    }

    /**
     * 根据父类类型获取最近一个实现的子类对象
     */
    public <T> T getBeanHierarchy(Class<T> beanType) throws BeansException {
        return beanFactory.getBeanHierarchy(beanType);
    }

    /**
     * 根据父类类型获取所有实现的子类对象，
     * 同时对列表进行{@link Order}注解排序
     */
    public <T> List<T> getBeanHierarchyList(Class<T> beanType) throws BeansException {
        // 获取实现类列表
        List<T> beanList = beanFactory.getBeanHierarchyList(beanType);
        // 对实现类进行Order注解排序，方便定义调用次序
        doSortByOrderAnnotation(beanList);
        return beanList;
    }

    /**
     * 根据Order注解进行列表的排序
     */
    private <T> void doSortByOrderAnnotation(List<T> compareList) {
        Collections.sort(compareList, new Comparator<T>() {
            @Override
            public int compare(T object1, T object2) {
                Order order1 = object1.getClass().getAnnotation(Order.class);
                Order order2 = object2.getClass().getAnnotation(Order.class);
                int order1Value = order1 == null ? 0 : order1.value();
                int order2Value = order2 == null ? 0 : order2.value();
                return order1Value - order2Value;
            }
        });
    }
}
