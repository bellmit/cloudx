package cloud.apposs.ioc;

import cloud.apposs.ioc.sample.bean.IProductBean;
import cloud.apposs.ioc.sample.bean.UserBean;
import cloud.apposs.ioc.sample.action.UserAction;
import cloud.apposs.ioc.sample.service.inf.IUserService;
import cloud.apposs.util.AntPathMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBeanFactory {
    private static final String BASE_PACKAGE = "cloud.apposs.ioc.sample.*";
    private BeanFactory factory = new BeanFactory();
    private AntPathMatcher apm = new AntPathMatcher();

    @Before
    public void before() {
        factory.load(BASE_PACKAGE);
    }

    @Test
    public void testAntPathMatcher() {
        Assert.assertTrue(apm.match("/etc/env/comm/*", "/etc/env/comm/aa.class"));
    }

    @Test
    public void testAntPathMatchStart() {
        Assert.assertTrue(apm.matchStart("cloud/apposs/booter/sample/**/*.class", "cloud/"));
    }

    /**
     * 默认IOC容器实例都是单例
     */
    @Test
    public void testBeanFactorySingleton() {
        UserAction user1 = factory.getBean(UserAction.class);
        UserAction user2 = factory.getBean(UserAction.class);
        Assert.assertTrue(user1 == user2);
    }

    /**
     * 声明IOC容器实例为Prototype则为非单例
     */
    @Test
    public void testBeanFactoryPrototype() {
        UserBean bean1 = factory.getBean(UserBean.class);
        UserBean bean2 = factory.getBean(UserBean.class);
        System.out.println(bean1);
        System.out.println(bean2);
        Assert.assertTrue(bean1 != bean2);
    }

    @Test
    public void testBeanFactorySuperClass() {
        IUserService service = factory.getBeanHierarchy(IUserService.class);
        Assert.assertTrue(service != null);
    }

    @Test
    public void testBeanFactoryInjectObject() {
        UserAction user = factory.getBean(UserAction.class);
        Assert.assertTrue(user.getUser() != null);
    }

    @Test
    public void testBeanFactoryInjectInterface() {
        UserAction user = factory.getBean(UserAction.class);
        IProductBean product = user.getProduct();
        Assert.assertTrue(product.getProductId() == 100);
        Assert.assertNotNull(user.getUserProduct());
    }
}
